package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueuePriorityServiceTest {
    private final QueueTicketRepository repository = mock(QueueTicketRepository.class);
    private final QueuePriorityService service = new QueuePriorityService(repository);
    private final LocalDate date = LocalDate.of(2026, 9, 2);
    private final LocalDateTime shiftStart = date.atTime(8, 0);
    private final Department room = Department.builder().departmentId(UUID.randomUUID())
            .name("Phòng khám Nội").departmentType(DepartmentType.EXAMINATION).build();
    private final MedicalService examination = MedicalService.builder().serviceId(UUID.randomUUID())
            .name("Khám Nội").departmentType(DepartmentType.EXAMINATION).build();

    @BeforeEach
    void returnCandidateAsFirstExaminationOfItsVisit() {
        when(repository.findAllByVisit_VisitId(any())).thenAnswer(invocation -> {
            UUID visitId = invocation.getArgument(0);
            return candidates.stream().filter(ticket -> visitId.equals(ticket.getVisit().getVisitId())).toList();
        });
    }

    private List<QueueTicket> candidates = List.of();

    private QueueTicket ticket(int queueNumber, QueueStatus status, LocalDateTime checkedInAt,
                               boolean hasAppointment) {
        Appointment appointment = hasAppointment ? Appointment.builder()
                .appointmentId(UUID.randomUUID()).scheduledAt(shiftStart).build() : null;
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID())
                .appointment(appointment).checkInTime(checkedInAt).build();
        QueueTicket ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(room).service(examination).workDate(date).queueNumber(queueNumber)
                .status(status).build();
        ticket.setCreatedAt(shiftStart.plusMinutes(queueNumber));
        return ticket;
    }

    @Test
    void protectsFifoHeadThenReturnsFromTestThenAlternatesAppointmentAndRegular() {
        QueueTicket protectedHead = ticket(1, QueueStatus.WAITING, shiftStart, false);
        QueueTicket appointment = ticket(4, QueueStatus.WAITING, shiftStart.plusMinutes(10), true);
        QueueTicket regular = ticket(2, QueueStatus.WAITING, shiftStart, false);
        QueueTicket returning = ticket(3, QueueStatus.TEST_DONE, shiftStart, false);
        candidates = List.of(protectedHead, regular, returning, appointment);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.plusMinutes(20));

        assertEquals(List.of(protectedHead, returning, appointment, regular),
                ranked.stream().map(QueuePriorityService.RankedTicket::ticket).toList());
        assertTrue(ranked.get(0).canCall());
        assertEquals(List.of(1, 2, 3, 4),
                ranked.stream().map(QueuePriorityService.RankedTicket::waitingPosition).toList());
        assertEquals(QueuePriorityService.RETURNING_FROM_TEST, ranked.get(1).priority().category());
        assertEquals(QueuePriorityService.APPOINTMENT_ON_TIME, ranked.get(2).priority().category());
    }

    @Test
    void lateAppointmentBecomesRegularAndEarlyAppointmentWaitsForItsShift() {
        QueueTicket late = ticket(1, QueueStatus.WAITING, shiftStart.plusMinutes(16), true);
        QueueTicket early = ticket(2, QueueStatus.WAITING, shiftStart.minusMinutes(30), true);
        candidates = List.of(late, early);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.minusMinutes(5));

        assertEquals(QueuePriorityService.APPOINTMENT_LATE, ranked.get(0).priority().category());
        assertFalse(ranked.get(0).priority().activeAppointmentPriority());
        assertEquals(QueuePriorityService.APPOINTMENT_ON_TIME, ranked.get(1).priority().category());
        assertEquals("Có lịch · chờ đến ca", ranked.get(1).priority().label());
        assertFalse(ranked.get(1).priority().activeAppointmentPriority());
    }

    @Test
    void calledTicketRemainsOnlyCallableTicket() {
        QueueTicket waiting = ticket(1, QueueStatus.WAITING, shiftStart, false);
        QueueTicket called = ticket(2, QueueStatus.CALLED, shiftStart, false);
        candidates = List.of(waiting, called);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.plusMinutes(5));

        assertTrue(ranked.stream().filter(item -> item.ticket() == called).findFirst().orElseThrow().canCall());
        assertFalse(ranked.stream().filter(item -> item.ticket() == waiting).findFirst().orElseThrow().canCall());
    }

    @Test
    void confirmedReturnIsNextWithoutInterruptingCalledPatient() {
        QueueTicket normal = ticket(1, QueueStatus.WAITING, shiftStart, false);
        QueueTicket returned = ticket(8, QueueStatus.WAITING, shiftStart, false);
        returned.setCalledAt(shiftStart.plusMinutes(2));
        candidates = List.of(normal, returned);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.plusMinutes(20));

        assertEquals(returned, ranked.get(0).ticket());
        assertEquals(QueuePriorityService.RETURNED_AFTER_ABSENCE, ranked.get(0).priority().category());
        assertTrue(ranked.get(0).canCall());

        QueueTicket called = ticket(9, QueueStatus.CALLED, shiftStart, false);
        candidates = List.of(normal, returned, called);
        ranked = service.rank(candidates, shiftStart.plusMinutes(20));
        assertTrue(ranked.stream().filter(item -> item.ticket() == called).findFirst().orElseThrow().canCall());
        assertFalse(ranked.stream().filter(item -> item.ticket() == returned).findFirst().orElseThrow().canCall());
    }
}
