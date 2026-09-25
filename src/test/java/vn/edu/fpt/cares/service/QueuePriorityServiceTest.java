package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.model.Appointment;
import vn.edu.fpt.cares.model.CustomerVisit;
import vn.edu.fpt.cares.model.Department;
import vn.edu.fpt.cares.model.MedicalService;
import vn.edu.fpt.cares.model.QueueTicket;
import vn.edu.fpt.cares.repository.QueueTicketRepository;
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
            return candidates.stream().filter(java.util.Objects::nonNull)
                    .filter(ticket -> visitId.equals(ticket.getVisit().getVisitId())).toList();
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
    void protectsFifoHeadThenReturnsFromTestThenPrioritizesAppointmentGroup() {
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
    void placesAllOnTimeAppointmentsBeforeRemainingRegularPatients() {
        QueueTicket protectedHead = ticket(1, QueueStatus.WAITING, shiftStart, false);
        QueueTicket regularSecond = ticket(2, QueueStatus.WAITING, shiftStart, false);
        QueueTicket appointmentFirst = ticket(3, QueueStatus.WAITING, shiftStart.plusMinutes(5), true);
        QueueTicket regularThird = ticket(4, QueueStatus.WAITING, shiftStart, false);
        QueueTicket appointmentSecond = ticket(5, QueueStatus.WAITING, shiftStart.plusMinutes(10), true);
        candidates = List.of(protectedHead, regularSecond, appointmentFirst, regularThird, appointmentSecond);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.plusMinutes(20));

        assertEquals(List.of(protectedHead, appointmentFirst, appointmentSecond, regularSecond, regularThird),
                ranked.stream().map(QueuePriorityService.RankedTicket::ticket).toList());
        assertEquals(List.of(1, 2, 3, 4, 5),
                ranked.stream().map(QueuePriorityService.RankedTicket::waitingPosition).toList());
        assertTrue(ranked.get(0).canCall());
        assertFalse(ranked.get(1).canCall());
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

    @Test
    void inProgressPatientKeepsRoomBusyAndNullCandidatesAreIgnored() {
        QueueTicket inProgress = ticket(2, QueueStatus.IN_PROGRESS, shiftStart, false);
        QueueTicket waiting = ticket(1, QueueStatus.WAITING, shiftStart, false);
        QueueTicket blocked = ticket(3, QueueStatus.BLOCKED, shiftStart, false);
        candidates = java.util.Arrays.asList(null, waiting, blocked, inProgress);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.plusMinutes(5));

        assertEquals(inProgress, ranked.get(0).ticket());
        assertFalse(ranked.stream().anyMatch(QueuePriorityService.RankedTicket::canCall));
        assertEquals(1, ranked.stream().filter(item -> item.ticket() == waiting)
                .findFirst().orElseThrow().waitingPosition());
        assertEquals(null, ranked.stream().filter(item -> item.ticket() == blocked)
                .findFirst().orElseThrow().waitingPosition());
        assertTrue(service.rank(null, shiftStart).isEmpty());
    }

    @Test
    void appointmentPriorityAppliesOnlyToFirstExaminationOnMatchingWorkDate() {
        QueueTicket first = ticket(1, QueueStatus.WAITING, shiftStart, true);
        QueueTicket second = ticket(2, QueueStatus.WAITING, shiftStart, false);
        second.getVisit().setAppointment(first.getVisit().getAppointment());
        second.setVisit(first.getVisit());
        QueueTicket wrongDate = ticket(3, QueueStatus.WAITING, shiftStart, true);
        wrongDate.setWorkDate(date.plusDays(1));
        QueueTicket noScheduledTime = ticket(4, QueueStatus.WAITING, shiftStart, true);
        noScheduledTime.getVisit().getAppointment().setScheduledAt(null);
        QueueTicket noCheckIn = ticket(5, QueueStatus.WAITING, null, true);
        QueueTicket departmentFallback = ticket(6, QueueStatus.WAITING, shiftStart, true);
        departmentFallback.setService(null);
        candidates = List.of(first, second, wrongDate, noScheduledTime, noCheckIn, departmentFallback);

        List<QueuePriorityService.RankedTicket> ranked = service.rank(candidates, shiftStart.plusMinutes(1));

        assertEquals(QueuePriorityService.APPOINTMENT_ON_TIME, priority(ranked, first).category());
        assertEquals(QueuePriorityService.REGULAR, priority(ranked, second).category());
        assertEquals(QueuePriorityService.REGULAR, priority(ranked, wrongDate).category());
        assertEquals(QueuePriorityService.REGULAR, priority(ranked, noScheduledTime).category());
        assertEquals(QueuePriorityService.APPOINTMENT_LATE, priority(ranked, noCheckIn).category());
        assertEquals(QueuePriorityService.APPOINTMENT_ON_TIME, priority(ranked, departmentFallback).category());
    }

    private QueuePriorityService.PriorityInfo priority(
            List<QueuePriorityService.RankedTicket> ranked, QueueTicket ticket) {
        return ranked.stream().filter(item -> item.ticket() == ticket)
                .findFirst().orElseThrow().priority();
    }
}
