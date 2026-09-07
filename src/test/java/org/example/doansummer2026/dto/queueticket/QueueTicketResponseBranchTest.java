package org.example.doansummer2026.dto.queueticket;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QueueTicketResponseBranchTest {

    @Test
    void mapsRegisteredPatientBusyTicketServicesAndSameRoomPosition() {
        Profile patient = Profile.builder().patientCode("BN-01").fullName("Nguyễn Anh Đức")
                .phone("0901").email("a@example.test").gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1990, 1, 2)).bloodType(BloodType.A_POSITIVE).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient)
                .checkInTime(LocalDateTime.of(2026, 9, 7, 8, 0)).build();
        Department department = Department.builder().departmentId(UUID.randomUUID()).name("Phòng Nội").build();
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("EX-01")
                .name("Khám Nội").price(BigDecimal.TEN).build();
        QueueTicket queue = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).department(department)
                .service(service).status(QueueStatus.WAITING).queueNumber(21).workDate(LocalDate.of(2026, 9, 7)).build();
        QueueTicket busy = QueueTicket.builder().department(department).status(QueueStatus.IN_PROGRESS)
                .calledAt(LocalDateTime.of(2026, 9, 7, 8, 5)).build();
        TestRequest completed = TestRequest.builder().service(service).status(TestRequestStatus.COMPLETED).build();
        completed.setCreatedAt(LocalDateTime.of(2026, 9, 7, 8, 2));
        TestRequest pendingWithoutService = TestRequest.builder().status(TestRequestStatus.PENDING).build();
        SameRoomExaminationChainResponse chain = new SameRoomExaminationChainResponse(
                visit.getVisitId(), department.getDepartmentId(), department.getName(), 9,
                queue.getTicketId(), 2, 4, 1, List.of());

        QueueTicketResponse response = QueueTicketResponse.from(queue, UUID.randomUUID(), 3, null,
                true, busy, List.of(pendingWithoutService, completed), chain);

        assertAll(
                () -> assertEquals(patient.getFullName(), response.patientName()),
                () -> assertEquals("2026-09-07", response.lastVisit()),
                () -> assertEquals(9, response.displayQueueNumber()),
                () -> assertEquals(2, response.examinationPosition()),
                () -> assertEquals(4, response.examinationTotalServices()),
                () -> assertFalse(response.canCall()),
                () -> assertEquals(department.getDepartmentId(), response.busyDepartmentId()),
                () -> assertEquals(1, response.completedServices()),
                () -> assertEquals("Cận lâm sàng", response.services().get(1).serviceName()));

        QueueTicketResponse prioritized = response.withQueuePriority(1, "RETURNED_AFTER_ABSENCE",
                "Đã quay lại", LocalDateTime.of(2026, 9, 7, 8, 30), true);
        assertFalse(prioritized.canCall(), "busy patient remains non-callable despite requested priority");
        assertEquals(1, prioritized.waitingPosition());
    }

    @Test
    void mapsGuestFallbackAndSingleServiceBranches() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = Appointment.builder().appointmentId(appointmentId)
                .guestFullName("Khách Vãng Lai").guestPhone("0999").build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).appointment(appointment).build();
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).serviceCode("IMG")
                .name("Siêu âm").build();
        QueueTicket queue = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit).service(service)
                .status(QueueStatus.TEST_DONE).queueNumber(5).build();

        QueueTicketResponse response = QueueTicketResponse.from(queue, null, null, null,
                false, null, null);
        assertAll(
                () -> assertEquals("Khách Vãng Lai", response.patientName()),
                () -> assertEquals("0999", response.patientPhone()),
                () -> assertTrue(response.patientCode().startsWith("BN-TAM-")),
                () -> assertEquals(1, response.services().size()),
                () -> assertTrue(response.canCall()),
                () -> assertNull(response.departmentId()),
                () -> assertNull(response.busyQueueStatus()));

        QueueTicketResponse withoutEverything = QueueTicketResponse.from(QueueTicket.builder()
                .ticketId(UUID.randomUUID()).status(QueueStatus.BLOCKED).build());
        assertNull(withoutEverything.visitId());
        assertTrue(withoutEverything.services().isEmpty());
        assertFalse(withoutEverything.canCall());

        Appointment anonymous = Appointment.builder().guestFullName("Ẩn danh").build();
        QueueTicketResponse anonymousResponse = QueueTicketResponse.from(QueueTicket.builder()
                .visit(CustomerVisit.builder().appointment(anonymous).build()).status(QueueStatus.CALLED).build());
        assertNull(anonymousResponse.patientCode());
    }
}
