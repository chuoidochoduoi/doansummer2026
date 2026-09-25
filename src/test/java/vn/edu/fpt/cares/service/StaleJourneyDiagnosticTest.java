package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.VisitStatus;
import vn.edu.fpt.cares.model.CustomerVisit;
import vn.edu.fpt.cares.model.Department;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.model.QueueTicket;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Characterization tests: reproduce existing behavior without starting Spring or accessing a database. */
class StaleJourneyDiagnosticTest {
    private final CustomerVisitRepository visits = mock(CustomerVisitRepository.class);
    private final QueueTicketRepository queues = mock(QueueTicketRepository.class);
    private final TestRequestRepository tests = mock(TestRequestRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final MedicalRecordRepository records = mock(MedicalRecordRepository.class);
    private final PatientJourneyService journeys = new PatientJourneyService(
            visits, queues, tests, invoices, records, mock(SimpMessagingTemplate.class),
            mock(QueuePriorityService.class));

    private CustomerVisit oldVisit(QueueStatus status) {
        var checkedIn = LocalDateTime.now().minusMinutes(10734);
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.IN_PROGRESS)
                .checkInTime(checkedIn).customer(Profile.builder().profileId(UUID.randomUUID())
                        .fullName("Bệnh nhân kiểm thử quá ngày").build()).build();
        var queue = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).status(status)
                .queueNumber(1).workDate(checkedIn.toLocalDate()).department(Department.builder().departmentId(UUID.randomUUID())
                        .name("Phòng Da liễu").roomCode("DER-104").departmentType(DepartmentType.EXAMINATION).build())
                .service(MedicalService.builder().serviceId(UUID.randomUUID()).name("Khám Da liễu").build()).build();
        queue.setCreatedAt(checkedIn);
        when(visits.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(queue));
        return visit;
    }

    @Test
    void inProgressAfter10734MinutesStillAppearsInProgress() {
        var visit = oldVisit(QueueStatus.IN_PROGRESS);
        var result = journeys.get(visit.getVisitId());
        assertEquals("IN_PROGRESS", result.currentStatus());
        assertTrue(result.waitingMinutes() >= 10734);
        assertTrue(result.warning());
        verify(visits, never()).save(any());
        verify(queues, never()).save(any());
    }

    @Test
    void advancingOldInProgressJourneyDoesNotFinishIt() {
        var visit = oldVisit(QueueStatus.IN_PROGRESS);
        when(visits.findByIdForUpdate(visit.getVisitId())).thenReturn(Optional.of(visit));
        journeys.activateNext(visit.getVisitId());
        assertEquals(VisitStatus.IN_PROGRESS, visit.getStatus());
        assertNull(visit.getCheckOutTime());
        verify(visits, never()).save(any());
        verify(queues, never()).save(any());
    }

    @Test
    void endingLastQueueAllowsVisitCompletionEvenAfterSeveralDays() {
        var visit = oldVisit(QueueStatus.DONE);
        when(visits.findByIdForUpdate(visit.getVisitId())).thenReturn(Optional.of(visit));
        journeys.activateNext(visit.getVisitId());
        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        assertNotNull(visit.getCheckOutTime());
        assertEquals("COMPLETED", journeys.get(visit.getVisitId()).currentStatus());
        verify(visits).save(visit);
    }

    @Test
    void overnightCleanupSkipsWaitingButExcludesInProgressAndTestDone() {
        var appointmentRepo = mock(AppointmentRepository.class);
        var cleanup = new SystemCleanupService(appointmentRepo, queues,
                mock(CustomerVisitRepository.class), mock(AuditLogService.class));
        var yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        var waiting = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.WAITING).workDate(yesterday).build();
        var examining = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.IN_PROGRESS).workDate(yesterday).build();
        var returning = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.TEST_DONE).workDate(yesterday).build();
        var waitingForTest = QueueTicket.builder().ticketId(UUID.randomUUID()).status(QueueStatus.WAITING_FOR_TEST).workDate(yesterday).build();
        when(queues.findOverdueActiveTickets(any(LocalDate.class), anyList())).thenAnswer(invocation -> {
            LocalDate today = invocation.getArgument(0);
            List<QueueStatus> statuses = invocation.getArgument(1);
            assertFalse(statuses.contains(QueueStatus.IN_PROGRESS));
            assertFalse(statuses.contains(QueueStatus.TEST_DONE));
            assertFalse(statuses.contains(QueueStatus.WAITING_FOR_TEST));
            return List.of(waiting, examining, returning, waitingForTest).stream()
                    .filter(queue -> queue.getWorkDate().isBefore(today) && statuses.contains(queue.getStatus())).toList();
        });
        cleanup.cleanupEndOfDay();
        assertEquals(QueueStatus.SKIPPED, waiting.getStatus());
        assertEquals(QueueStatus.IN_PROGRESS, examining.getStatus());
        assertEquals(QueueStatus.TEST_DONE, returning.getStatus());
        assertEquals(QueueStatus.WAITING_FOR_TEST, waitingForTest.getStatus());
        verify(queues).saveAll(List.of(waiting));
    }

    @Test
    void overnightCleanupCancelsVisitWhenNoServiceWasCompleted() {
        var appointmentRepo = mock(AppointmentRepository.class);
        var visitRepo = mock(CustomerVisitRepository.class);
        var cleanup = new SystemCleanupService(appointmentRepo, queues, visitRepo,
                mock(AuditLogService.class));
        var yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.CHECKED_IN)
                .checkInTime(yesterday.atTime(8, 0)).build();
        var waiting = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .status(QueueStatus.WAITING).workDate(yesterday).build();
        when(queues.findOverdueActiveTickets(any(), anyList())).thenReturn(List.of(waiting));
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(waiting));

        cleanup.cleanupEndOfDay();

        assertEquals(QueueStatus.SKIPPED, waiting.getStatus());
        assertEquals(VisitStatus.CANCELLED, visit.getStatus());
        assertNotNull(visit.getCheckOutTime());
        verify(visitRepo).saveAll(List.of(visit));
    }

    @Test
    void overnightCleanupCompletesPartialVisitAndKeepsSkippedServiceHistory() {
        var appointmentRepo = mock(AppointmentRepository.class);
        var visitRepo = mock(CustomerVisitRepository.class);
        var cleanup = new SystemCleanupService(appointmentRepo, queues, visitRepo,
                mock(AuditLogService.class));
        var yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.IN_PROGRESS)
                .checkInTime(yesterday.atTime(8, 0)).build();
        var waiting = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .status(QueueStatus.WAITING).workDate(yesterday).build();
        var done = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .status(QueueStatus.DONE).workDate(yesterday).build();
        when(queues.findOverdueActiveTickets(any(), anyList())).thenReturn(List.of(waiting));
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(done, waiting));

        cleanup.cleanupEndOfDay();

        assertEquals(QueueStatus.SKIPPED, waiting.getStatus());
        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        assertNotNull(visit.getCheckOutTime());
    }

    @Test
    void visitAlreadySkippedBeforeMidnightIsStillClosed() {
        var appointmentRepo = mock(AppointmentRepository.class);
        var visitRepo = mock(CustomerVisitRepository.class);
        var cleanup = new SystemCleanupService(appointmentRepo, queues, visitRepo,
                mock(AuditLogService.class));
        var yesterday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        var visit = CustomerVisit.builder().visitId(UUID.randomUUID()).status(VisitStatus.CHECKED_IN)
                .checkInTime(yesterday.atTime(8, 0)).build();
        var skipped = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .status(QueueStatus.SKIPPED).workDate(yesterday).build();
        when(queues.findOverdueActiveTickets(any(), anyList())).thenAnswer(invocation -> {
            List<QueueStatus> statuses = invocation.getArgument(1);
            return statuses.contains(QueueStatus.SKIPPED) ? List.of(skipped) : List.of();
        });
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(skipped));

        cleanup.cleanupEndOfDay();

        assertEquals(VisitStatus.CANCELLED, visit.getStatus());
        assertNotNull(visit.getCheckOutTime());
    }
}
