package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceAvailabilityServiceTest {
    @Mock StaffScheduleRepository scheduleRepository;
    @Mock StaffCapabilityRepository capabilityRepository;
    @Mock MedicalServiceRepository medicalServiceRepository;
    @Mock ShiftConfigRepository shiftRepository;
    @Mock ShiftScheduleResolver shiftResolver;
    @InjectMocks ServiceAvailabilityService service;

    private final LocalDate date = LocalDate.of(2026, 9, 10);
    private ShiftConfig shift;
    private Department department;
    private StaffInfo staff;

    @BeforeEach
    void setUp() {
        shift = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Morning").isActive(true).build();
        department = Department.builder().departmentId(UUID.randomUUID()).name("Cardiology")
                .roomCode("R-1").departmentType(DepartmentType.EXAMINATION)
                .status(DepartmentStatus.AVAILABLE).build();
        Account account = Account.builder().isActive(true).build();
        Profile profile = Profile.builder().fullName("Doctor A").account(account).build();
        staff = StaffInfo.builder().staffId(UUID.randomUUID()).staffCode("STF-1")
                .profile(profile).systemRole(SystemRole.DOCTOR).department(department).build();
        when(shiftResolver.resolve(shift, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.of(7, 30), LocalTime.of(11, 30), ShiftTimeSource.NORMAL, null));
    }

    @Test
    void examinationIsAvailableWithScheduledDoctorInSpecialization() {
        Specialization specialization = Specialization.builder().specializationId(UUID.randomUUID()).active(true).build();
        staff.setSpecialization(specialization);
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).name("Exam")
                .serviceCode("EX-1").status(ServiceStatus.ACTIVE).allowCustomerBooking(true)
                .departmentType(DepartmentType.EXAMINATION).department(department)
                .requiredSpecialization(specialization).build();
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build()));
        assertTrue(service.evaluate(medicalService, date, shift, true).available());
    }

    @Test
    void examinationIsUnavailableWithoutQualifiedStaff() {
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).name("Exam")
                .serviceCode("EX-1").status(ServiceStatus.ACTIVE).allowCustomerBooking(true)
                .departmentType(DepartmentType.EXAMINATION).department(department).build();
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of());
        var result = service.evaluate(medicalService, date, shift, true);
        assertFalse(result.available());
        assertEquals(ShiftUnavailableReason.NO_QUALIFIED_STAFF, result.reason());
    }

    @Test
    void expiredCapabilityDoesNotCoverParaclinicalService() {
        ServiceCapability capability = ServiceCapability.builder().capabilityId(UUID.randomUUID())
                .name("Ultrasound").active(true).build();
        department.getCapabilities().add(capability);
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).name("Ultrasound")
                .serviceCode("US-1").status(ServiceStatus.ACTIVE).allowCustomerBooking(true)
                .departmentType(DepartmentType.PARACLINICAL).department(department)
                .requiredCapability(capability).build();
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build()));
        when(capabilityRepository.findAllByStaff_StaffIdAndStatus(staff.getStaffId(), StaffCapabilityStatus.ACTIVE))
                .thenReturn(List.of(StaffCapability.builder().staff(staff).capability(capability)
                        .status(StaffCapabilityStatus.ACTIVE).expiryDate(date.minusDays(1)).build()));
        assertEquals(ShiftUnavailableReason.NO_QUALIFIED_STAFF,
                service.evaluate(medicalService, date, shift, true).reason());
    }

    @Test
    void capabilityValidOnServiceDateCoversService() {
        ServiceCapability capability = ServiceCapability.builder().capabilityId(UUID.randomUUID())
                .name("ECG").active(true).build();
        department.getCapabilities().add(capability);
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).name("ECG")
                .serviceCode("ECG-1").status(ServiceStatus.ACTIVE).allowCustomerBooking(true)
                .departmentType(DepartmentType.PARACLINICAL).department(department)
                .requiredCapability(capability).build();
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build()));
        when(capabilityRepository.findAllByStaff_StaffIdAndStatus(staff.getStaffId(), StaffCapabilityStatus.ACTIVE))
                .thenReturn(List.of(StaffCapability.builder().staff(staff).capability(capability)
                        .status(StaffCapabilityStatus.ACTIVE).expiryDate(date).build()));
        assertTrue(service.evaluate(medicalService, date, shift, true).available());
    }
}
