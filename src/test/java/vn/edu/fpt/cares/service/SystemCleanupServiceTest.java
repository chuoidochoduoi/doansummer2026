package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.AppointmentStatus;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.VisitStatus;
import vn.edu.fpt.cares.model.Appointment;
import vn.edu.fpt.cares.model.CustomerVisit;
import vn.edu.fpt.cares.model.QueueTicket;
import vn.edu.fpt.cares.repository.AppointmentRepository;
import vn.edu.fpt.cares.repository.CustomerVisitRepository;
import vn.edu.fpt.cares.repository.QueueTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemCleanupServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock QueueTicketRepository queueTicketRepository;
    @Mock CustomerVisitRepository customerVisitRepository;
    @Mock AuditLogService auditLogService;
    @InjectMocks SystemCleanupService service;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    void cleanupClosesOnlyExpiredItemsWithoutActiveClinicalWork() {
        Appointment pending = appointment(AppointmentStatus.PENDING, today.minusDays(1).atTime(8, 0));
        Appointment rescheduled = appointment(AppointmentStatus.RESCHEDULED, today.minusDays(2).atTime(9, 0));
        Appointment todayAppointment = appointment(AppointmentStatus.PENDING, today.atTime(7, 30));
        Appointment checkedIn = appointment(AppointmentStatus.CHECKED_IN, today.minusDays(1).atTime(7, 30));
        Appointment missingTime = appointment(AppointmentStatus.PENDING, null);
        when(appointmentRepository.findAll()).thenReturn(
                List.of(pending, rescheduled, todayAppointment, checkedIn, missingTime));

        QueueTicket waiting = ticket(QueueStatus.WAITING, null);
        when(queueTicketRepository.findOverdueActiveTickets(today,
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.BLOCKED)))
                .thenReturn(List.of(waiting));

        CustomerVisit partiallyCompleted = visit(VisitStatus.IN_PROGRESS);
        CustomerVisit abandoned = visit(VisitStatus.IN_PROGRESS);
        CustomerVisit activeClinicalWork = visit(VisitStatus.IN_PROGRESS);
        CustomerVisit alreadyClosed = visit(VisitStatus.COMPLETED);
        CustomerVisit alreadyCancelled = visit(VisitStatus.CANCELLED);
        CustomerVisit awaitingResult = visit(VisitStatus.IN_PROGRESS);
        QueueTicket skippedPartial = ticket(QueueStatus.SKIPPED, partiallyCompleted);
        QueueTicket skippedPartialDuplicate = ticket(QueueStatus.SKIPPED, partiallyCompleted);
        QueueTicket skippedAbandoned = ticket(QueueStatus.SKIPPED, abandoned);
        QueueTicket skippedActive = ticket(QueueStatus.SKIPPED, activeClinicalWork);
        QueueTicket skippedClosed = ticket(QueueStatus.SKIPPED, alreadyClosed);
        QueueTicket skippedCancelled = ticket(QueueStatus.SKIPPED, alreadyCancelled);
        QueueTicket skippedAwaitingResult = ticket(QueueStatus.SKIPPED, awaitingResult);
        QueueTicket skippedWithoutVisit = ticket(QueueStatus.SKIPPED, null);
        when(queueTicketRepository.findOverdueActiveTickets(today, List.of(QueueStatus.SKIPPED)))
                .thenReturn(List.of(skippedPartial, skippedPartialDuplicate, skippedAbandoned,
                        skippedActive, skippedClosed, skippedCancelled, skippedAwaitingResult,
                        skippedWithoutVisit));

        when(queueTicketRepository.findAllByVisit_VisitId(partiallyCompleted.getVisitId()))
                .thenReturn(List.of(ticket(QueueStatus.DONE, partiallyCompleted), skippedPartial));
        when(queueTicketRepository.findAllByVisit_VisitId(abandoned.getVisitId()))
                .thenReturn(List.of(skippedAbandoned));
        when(queueTicketRepository.findAllByVisit_VisitId(activeClinicalWork.getVisitId()))
                .thenReturn(List.of(ticket(QueueStatus.WAITING_FOR_TEST, activeClinicalWork)));
        when(queueTicketRepository.findAllByVisit_VisitId(awaitingResult.getVisitId()))
                .thenReturn(List.of(ticket(QueueStatus.TEST_DONE, awaitingResult)));

        service.cleanupEndOfDay();

        assertAll(
                () -> assertEquals(AppointmentStatus.CANCELLED, pending.getStatus()),
                () -> assertEquals(AppointmentStatus.CANCELLED, rescheduled.getStatus()),
                () -> assertEquals(AppointmentStatus.PENDING, todayAppointment.getStatus()),
                () -> assertEquals(AppointmentStatus.CHECKED_IN, checkedIn.getStatus()),
                () -> assertEquals(QueueStatus.SKIPPED, waiting.getStatus()),
                () -> assertEquals(VisitStatus.COMPLETED, partiallyCompleted.getStatus()),
                () -> assertEquals(VisitStatus.CANCELLED, abandoned.getStatus()),
                () -> assertEquals(VisitStatus.IN_PROGRESS, activeClinicalWork.getStatus()),
                () -> assertNotNull(partiallyCompleted.getCheckOutTime()),
                () -> assertNotNull(abandoned.getCheckOutTime())
        );
        ArgumentCaptor<List<CustomerVisit>> visits = ArgumentCaptor.forClass(List.class);
        verify(customerVisitRepository).saveAll(visits.capture());
        assertEquals(2, visits.getValue().size());
        verify(auditLogService, times(5)).create(any());
    }

    @Test
    void cleanupHandlesAnEmptyDay() {
        when(appointmentRepository.findAll()).thenReturn(List.of());
        when(queueTicketRepository.findOverdueActiveTickets(today,
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.BLOCKED)))
                .thenReturn(List.of());
        when(queueTicketRepository.findOverdueActiveTickets(today, List.of(QueueStatus.SKIPPED)))
                .thenReturn(List.of());

        assertDoesNotThrow(service::cleanupEndOfDay);

        verify(appointmentRepository).saveAll(List.of());
        verify(queueTicketRepository).saveAll(List.of());
        verify(customerVisitRepository).saveAll(List.of());
        verifyNoInteractions(auditLogService);
    }

    private Appointment appointment(AppointmentStatus status, LocalDateTime scheduledAt) {
        return Appointment.builder().appointmentId(UUID.randomUUID()).status(status)
                .scheduledAt(scheduledAt).build();
    }

    private CustomerVisit visit(VisitStatus status) {
        return CustomerVisit.builder().visitId(UUID.randomUUID()).status(status).build();
    }

    private QueueTicket ticket(QueueStatus status, CustomerVisit visit) {
        return QueueTicket.builder().ticketId(UUID.randomUUID()).status(status).visit(visit).build();
    }
}
