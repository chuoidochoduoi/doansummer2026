package vn.edu.fpt.cares.dto;

import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentDetailResponse;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentResponse;
import vn.edu.fpt.cares.dto.medicalhistory.MedicalHistoryResponse;
import vn.edu.fpt.cares.dto.medicalrecord.ReceptionistRecordResponse;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResponseMappingTest {
    @Test
    void appointmentSummaryMapsServicesQueueCustomerAndCompletedStatus() {
        UUID owner = UUID.randomUUID();
        Profile customer = Profile.builder().profileId(owner).patientCode("BN-001").fullName("Nguyễn Anh Đức").build();
        Department department = Department.builder().name("Nội khoa").build();
        MedicalService blank = MedicalService.builder().name(" ").department(department).price(BigDecimal.ZERO).build();
        MedicalService first = MedicalService.builder().serviceId(UUID.randomUUID()).name("Khám Nội")
                .department(department).price(new BigDecimal("200000")).build();
        CustomerVisit visit = CustomerVisit.builder().status(VisitStatus.COMPLETED)
                .queueTickets(List.of(QueueTicket.builder().queueNumber(12).build())).build();
        Appointment appointment = Appointment.builder().appointmentId(UUID.randomUUID()).customer(customer)
                .scheduledAt(LocalDateTime.of(2026, 9, 5, 9, 0)).shiftName("Ca sáng").shiftTime("08:00 - 12:00")
                .status(AppointmentStatus.CHECKED_IN).services(new LinkedHashSet<>(List.of(first, blank))).visit(visit).build();
        var response = CustomerAppointmentResponse.from(appointment, owner, "SELF");
        assertAll(() -> assertEquals("05/09/2026", response.date()),
                () -> assertEquals("Nội khoa", response.specialty()),
                () -> assertEquals("Khám Nội", response.serviceSummary()),
                () -> assertEquals("12", response.queueNumber()),
                () -> assertEquals("completed", response.status()), () -> assertTrue(response.isSelf()));
    }

    @Test
    void appointmentSummaryCoversGuestDefaultsCancelledCheckedInAndMultipleServices() {
        Appointment guest = Appointment.builder().appointmentId(UUID.randomUUID()).guestFullName("Khách vãng lai")
                .scheduledAt(null).shiftName(null).shiftTime(null).status(AppointmentStatus.CANCELLED)
                .services(Set.of()).build();
        var cancelled = CustomerAppointmentResponse.from(guest);
        assertEquals("cancelled", cancelled.status());
        assertEquals("Chưa chọn dịch vụ", cancelled.serviceSummary());
        assertEquals("Khám Bệnh Chung", cancelled.specialty());
        assertEquals("", cancelled.date());
        assertEquals("Khách vãng lai", cancelled.patientName());

        MedicalService a = MedicalService.builder().name("A").price(BigDecimal.ONE).build();
        MedicalService b = MedicalService.builder().name("B").price(BigDecimal.TEN).build();
        guest.setServices(Set.of(b, a));
        guest.setStatus(AppointmentStatus.CHECKED_IN);
        assertEquals("A + 1 dịch vụ khác", CustomerAppointmentResponse.from(guest).serviceSummary());
        assertEquals("checked_in", CustomerAppointmentResponse.from(guest).status());
        guest.setStatus(AppointmentStatus.PENDING);
        assertEquals("upcoming", CustomerAppointmentResponse.from(guest).status());
        guest.setStatus(null);
        assertEquals("upcoming", CustomerAppointmentResponse.from(guest).status());
        guest.setServices(null);
        assertEquals("Chưa chọn dịch vụ", CustomerAppointmentResponse.from(guest).serviceSummary());
    }

    @Test
    void appointmentDetailMapsTimeVariantsServicesAndStatuses() {
        Profile customer = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức").build();
        MedicalService service = MedicalService.builder().serviceId(UUID.randomUUID()).name("Khám Tim")
                .price(new BigDecimal("250000")).build();
        Appointment appointment = Appointment.builder().appointmentId(UUID.randomUUID()).customer(customer)
                .scheduledAt(LocalDateTime.of(2026, 9, 5, 9, 0)).shiftTime("08:00 - 12:00").shiftName("Ca sáng")
                .status(AppointmentStatus.PENDING).services(Set.of(service)).build();
        var response = CustomerAppointmentDetailResponse.from(appointment, customer.getProfileId(), "SELF");
        assertEquals("08:00 - 12:00 (Ca sáng)", response.timeSlot());
        assertEquals(1, response.services().size());
        assertEquals(new BigDecimal("250000"), response.services().get(0).cost());

        appointment.setShiftName(null);
        assertEquals("08:00 - 12:00", CustomerAppointmentDetailResponse.from(appointment).timeSlot());
        appointment.setShiftTime(null);
        assertEquals("", CustomerAppointmentDetailResponse.from(appointment).timeSlot());
        appointment.setServices(null);
        assertTrue(CustomerAppointmentDetailResponse.from(appointment).services().isEmpty());
        appointment.setStatus(AppointmentStatus.CANCELLED);
        assertEquals("cancelled", CustomerAppointmentDetailResponse.from(appointment).status());
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        assertEquals("checked_in", CustomerAppointmentDetailResponse.from(appointment).status());

        appointment.setVisit(CustomerVisit.builder().status(VisitStatus.COMPLETED)
                .queueTickets(List.of(QueueTicket.builder().queueNumber(7).build())).build());
        assertEquals("completed", CustomerAppointmentDetailResponse.from(appointment).status());
        assertEquals("7", CustomerAppointmentDetailResponse.from(appointment).queueNumber());
        appointment.setStatus(null);
        assertEquals("upcoming", CustomerAppointmentDetailResponse.from(appointment).status());

        appointment.setCustomer(null);
        appointment.setGuestFullName("Khách vãng lai");
        var guest = CustomerAppointmentDetailResponse.from(appointment, UUID.randomUUID(), null);
        assertEquals("Khách vãng lai", guest.patientName());
        assertNull(guest.patientProfileId());
        assertNull(guest.patientCode());
        assertFalse(guest.isSelf());

        appointment.setVisit(CustomerVisit.builder().queueTickets(null).build());
        assertNull(CustomerAppointmentDetailResponse.from(appointment).queueNumber());
        appointment.setVisit(null);
        assertNull(CustomerAppointmentDetailResponse.from(appointment).queueNumber());
    }

    @Test
    void receptionistRecordMapsRegisteredCustomerGuestAndMissingVisit() {
        Profile customer = Profile.builder().fullName("Nguyễn Anh Đức").phone("0987654321")
                .dateOfBirth(LocalDate.now().minusYears(25)).gender(Gender.MALE).bloodType(BloodType.A_POSITIVE).build();
        CustomerVisit visit = CustomerVisit.builder().customer(customer)
                .checkInTime(LocalDateTime.of(2026, 9, 5, 9, 0)).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-001")
                .visit(visit).chiefComplaint("Đau đầu").build();
        var registered = ReceptionistRecordResponse.from(record);
        assertEquals("Nguyễn Anh Đức", registered.fullName());
        assertEquals("Nam", registered.gender());
        assertEquals("A+", registered.bloodType());
        assertEquals(25, registered.age());

        Appointment guestAppointment = Appointment.builder().isGuest(true).guestFullName("Khách A")
                .guestPhone("0912345678").guestAge(33).guestGender(Gender.FEMALE).build();
        record.setVisit(CustomerVisit.builder().appointment(guestAppointment).checkInTime(null).build());
        var guest = ReceptionistRecordResponse.from(record);
        assertEquals("Khách A", guest.fullName());
        assertEquals("Nữ", guest.gender());
        assertNull(guest.bloodType());
        record.setVisit(null);
        var missing = ReceptionistRecordResponse.from(record);
        assertNull(missing.fullName());
        assertNull(missing.lastVisitDate());
    }

    @Test
    void medicalHistoryMapsQueueAppointmentFallbackDoctorStatusesAndNoVisit() {
        UUID visitId = UUID.randomUUID();
        MedicalService service = MedicalService.builder().name("Khám Nội").build();
        StaffInfo doctor = StaffInfo.builder().profile(Profile.builder().fullName("Đỗ Anh Tuấn").build()).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(visitId)
                .checkInTime(LocalDateTime.of(2026, 9, 5, 9, 15)).build();
        MedicalRecord record = MedicalRecord.builder().recordId(UUID.randomUUID()).recordCode("MR-01")
                .visit(visit).queueTicket(QueueTicket.builder().service(service).build()).doctor(doctor)
                .diagnosis("Viêm họng").status(MedicalRecordStatus.COMPLETED).build();
        var response = MedicalHistoryResponse.from(record);
        assertEquals("Khám Nội", response.specialty());
        assertEquals("BS. Đỗ Anh Tuấn", response.doctor());
        assertEquals("completed", response.status());
        assertNotNull(response.visitCode());

        record.setQueueTicket(QueueTicket.builder().service(null).build());
        record.setStatus(MedicalRecordStatus.DRAFT);
        Appointment appointment = Appointment.builder().services(Set.of(MedicalService.builder().name(null).build())).build();
        visit.setAppointment(appointment);
        assertEquals("Khám bệnh", MedicalHistoryResponse.from(record).specialty());
        assertEquals("draft", MedicalHistoryResponse.from(record).status());

        record.setVisit(null);
        record.setQueueTicket(null);
        record.setDoctor(null);
        record.setStatus(MedicalRecordStatus.IN_PROGRESS);
        var noVisit = MedicalHistoryResponse.from(record);
        assertEquals("PARACLINICAL", noVisit.specialty());
        assertEquals("pending", noVisit.status());
        assertNull(noVisit.visitCode());
    }
}
