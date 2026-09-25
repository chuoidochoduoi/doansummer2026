package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.journey.PatientQueueResponse;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientQueueServiceTest {
    @Mock CustomerVisitRepository visitRepo;
    @Mock QueueTicketRepository queueRepo;
    @Mock TestRequestRepository testRepo;
    @Mock InvoiceRepository invoiceRepo;
    @Mock MedicalRecordRepository recordRepo;
    @Mock NotificationRepository notificationRepo;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock QueuePriorityService queuePriorityService;
    @InjectMocks PatientJourneyService service;

    final Profile patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Người được khám").build();
    final CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient).build();
    final Department room = Department.builder().departmentId(UUID.randomUUID()).name("Nội khoa").roomCode("N01").build();
    final LocalDate date = LocalDate.of(2026, 8, 31);
    final List<QueueStatus> ready = List.of(QueueStatus.WAITING, QueueStatus.TEST_DONE,
            QueueStatus.CALLED, QueueStatus.IN_PROGRESS, QueueStatus.WAITING_FOR_TEST);

    QueueTicket ticket(int number, QueueStatus status, CustomerVisit owner, Department department) {
        var ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).queueNumber(number).status(status)
                .visit(owner).department(department).workDate(date).build();
        ticket.setCreatedAt(LocalDateTime.of(2026, 8, 31, 8, 0).plusMinutes(number));
        return ticket;
    }

    void ownTickets(QueueTicket... tickets) {
        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(tickets));
    }

    void roomQueue(Department department, QueueTicket... tickets) {
        when(queueRepo.findWaitingPrioritized(department.getDepartmentId(), date, ready, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(tickets)));
        when(queuePriorityService.rank(anyList())).thenAnswer(invocation -> {
            List<QueueTicket> source = invocation.getArgument(0);
            int[] waitingPosition = {0};
            return source.stream().map(ticket -> {
                Integer position = ticket.getStatus() == QueueStatus.WAITING
                        || ticket.getStatus() == QueueStatus.TEST_DONE ? ++waitingPosition[0] : null;
                String category = ticket.getStatus() == QueueStatus.TEST_DONE
                        ? QueuePriorityService.RETURNING_FROM_TEST : QueuePriorityService.REGULAR;
                String label = ticket.getStatus() == QueueStatus.TEST_DONE
                        ? "Quay lại bác sĩ" : "Khách trực tiếp";
                return new QueuePriorityService.RankedTicket(ticket, position, false,
                        new QueuePriorityService.PriorityInfo(category, label, null, false));
            }).toList();
        });
    }

    PatientQueueResponse read() { return service.queueForCustomer(visit.getVisitId(), List.of(patient.getProfileId())); }

    @Test void waitingPositionExcludesServingAndKeepsStaffFifoOrder() {
        var other = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        var serving = ticket(1, QueueStatus.IN_PROGRESS, other, room);
        var called = ticket(2, QueueStatus.CALLED, other, room);
        var before = ticket(3, QueueStatus.TEST_DONE, other, room);
        var self = ticket(8, QueueStatus.WAITING, visit, room);
        var after = ticket(9, QueueStatus.WAITING, other, room);
        ownTickets(self);
        roomQueue(room, serving, called, before, self, after);
        var result = read();
        assertEquals(2, result.waitingPosition());
        assertEquals(1, result.peopleAhead());
        assertEquals(2, result.serving().size());
        assertEquals(List.of(false, true, false), result.waiting().stream().map(PatientQueueResponse.Entry::self).toList());
        assertEquals(List.of(1, 2, 3), result.waiting().stream().map(PatientQueueResponse.Entry::position).toList());
        verify(queueRepo).findWaitingPrioritized(room.getDepartmentId(), date, ready, Pageable.unpaged());
        verifyNoInteractions(messagingTemplate);
    }

    @Test void calledPatientHasNoWaitingPosition() {
        var self = ticket(8, QueueStatus.CALLED, visit, room);
        ownTickets(self); roomQueue(room, self);
        var result = read();
        assertNull(result.waitingPosition());
        assertNull(result.peopleAhead());
        assertTrue(result.waiting().isEmpty());
        assertTrue(result.serving().get(0).self());
    }

    @Test void returningPatientUsesOriginalRoomQueue() {
        var self = ticket(8, QueueStatus.TEST_DONE, visit, room);
        var lab = Department.builder().departmentId(UUID.randomUUID()).name("Xét nghiệm")
                .roomCode("LAB-1").departmentType(DepartmentType.PARACLINICAL).build();
        var labTicket = ticket(9, QueueStatus.DONE, visit, lab);
        var record = MedicalRecord.builder().recordId(UUID.randomUUID()).visit(visit)
                .queueTicket(self).status(MedicalRecordStatus.IN_PROGRESS).build();
        var invoice = Invoice.builder().invoiceId(UUID.randomUUID()).visit(visit)
                .medicalRecord(record).status(InvoiceStatus.PAID).build();
        invoice.setCreatedAt(date.atTime(9, 0));
        var completedTest = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record)
                .queueTicket(labTicket).performingDepartment(lab).status(TestRequestStatus.COMPLETED)
                .invoiceItem(InvoiceItem.builder().invoice(invoice).build()).build();
        completedTest.setCreatedAt(date.atTime(9, 10));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(invoice));
        when(testRepo.findAllByMedicalRecord_Visit_VisitId(visit.getVisitId())).thenReturn(List.of(completedTest));
        when(recordRepo.findByQueueTicket_TicketId(self.getTicketId())).thenReturn(Optional.of(record));
        ownTickets(self, labTicket); roomQueue(room, self);
        var result = read();
        assertEquals("TEST_DONE", result.currentStatus());
        assertEquals(1, result.waitingPosition());
        assertEquals(0, result.peopleAhead());
    }

    @Test void activeLabTakesPrecedenceOverSuspendedExamination() {
        var lab = Department.builder().departmentId(UUID.randomUUID()).name("Xét nghiệm").roomCode("XN01")
                .departmentType(DepartmentType.PARACLINICAL).build();
        var examination = ticket(1, QueueStatus.WAITING_FOR_TEST, visit, room);
        var self = ticket(2, QueueStatus.WAITING, visit, lab);
        ownTickets(examination, self); roomQueue(lab, self);
        assertEquals("XN01", read().roomCode());
        verify(queueRepo, never()).findWaitingPrioritized(room.getDepartmentId(), date, ready, Pageable.unpaged());
    }

    @Test void pendingPaymentDoesNotExposeRoomQueue() {
        ownTickets(ticket(1, QueueStatus.WAITING, visit, room));
        when(invoiceRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(
                Invoice.builder().invoiceId(UUID.randomUUID()).status(InvoiceStatus.PENDING).build()));
        var result = read();
        assertEquals("PAYMENT_PENDING", result.currentStatus());
        assertNull(result.roomName());
        assertTrue(result.waiting().isEmpty());
    }

    @Test void outsideFamilyDeniedBeforeReadingJourneyOrRoom() {
        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        assertThrows(AccessDeniedException.class, () -> service.queueForCustomer(visit.getVisitId(), List.of(UUID.randomUUID())));
        verifyNoInteractions(queueRepo, invoiceRepo, testRepo);
    }

    @Test void completedVisitDoesNotExposeLiveRoomQueue() {
        visit.setStatus(VisitStatus.COMPLETED);
        when(visitRepo.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        assertNull(read().waitingPosition());
        verify(queueRepo).findAllByVisit_VisitId(visit.getVisitId());
        verifyNoInteractions(invoiceRepo, testRepo);
    }

    @Test void anonymousEntryContainsNoIdentifyingOrClinicalFields() {
        assertEquals(List.of("position", "self", "status"), Arrays.stream(PatientQueueResponse.Entry.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList());
    }

    @Test void guestQueueRequiresMatchingPhoneAndVisitCode() {
        var self = ticket(5, QueueStatus.WAITING, visit, room);
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
                .thenReturn(List.of(visit));
        when(queueRepo.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(self));
        roomQueue(room, self);
        String code = "VIS-" + visit.getVisitId().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        var result = service.lookupGuestQueue(" " + code.toLowerCase(java.util.Locale.ROOT) + " ", "090 123 4567");
        assertEquals(visit.getVisitId(), result.visitId());
        assertEquals(1, result.waitingPosition());
        assertTrue(result.waiting().get(0).self());
        verifyNoInteractions(messagingTemplate);
    }

    @Test void guestWrongCodeDoesNotReadRoomOrJourney() {
        when(visitRepo.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0901234567"))
                .thenReturn(List.of(visit));
        String code = visit.getVisitId().toString().startsWith("ffffffff") ? "VIS-00000000" : "VIS-FFFFFFFF";
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class,
                () -> service.lookupGuestQueue(code, "0901234567"));
        verifyNoInteractions(queueRepo, invoiceRepo, testRepo);
    }

    @Test void guestWrongPhoneDoesNotReadRoomOrJourney() {
        String code = "VIS-" + visit.getVisitId().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class,
                () -> service.lookupGuestQueue(code, "0999999999"));
        verifyNoInteractions(queueRepo, invoiceRepo, testRepo);
    }

    @Test void guestInvalidCodeRejectedBeforeDatabaseRead() {
        assertThrows(vn.edu.fpt.cares.exception.ResourceNotFoundException.class,
                () -> service.lookupGuestQueue(visit.getVisitId().toString(), "0901234567"));
        verifyNoInteractions(visitRepo, queueRepo, invoiceRepo, testRepo);
    }
}
