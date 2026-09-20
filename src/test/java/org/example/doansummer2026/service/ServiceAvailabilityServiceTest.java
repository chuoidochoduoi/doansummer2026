package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @Mock DepartmentRepository departmentRepository;
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
        lenient().when(shiftResolver.resolve(shift, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                shift, null, LocalTime.of(7, 30), LocalTime.of(11, 30), ShiftTimeSource.NORMAL, null));
    }

    @Test
    void examinationIsAvailableWithScheduledDoctorInSpecialization() {
        Specialization specialization = Specialization.builder().specializationId(UUID.randomUUID()).active(true).build();
        staff.setSpecialization(specialization);
        department.setSpecialization(specialization);
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

    @ParameterizedTest @ValueSource(strings={"inactiveService","unavailableRoom","missingCapability","inactiveCapability","wrongSpecialization"})
    void unavailableConfigurationRejectsBeforeStaffLookup(String reason) {
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.EXAMINATION).department(department).build();
        ShiftUnavailableReason expected;
        if(reason.equals("inactiveService")) { medicalService.setStatus(null); expected=ShiftUnavailableReason.SERVICE_INACTIVE; }
        else if(reason.equals("unavailableRoom")) { department.setStatus(null); expected=ShiftUnavailableReason.DEPARTMENT_UNAVAILABLE; }
        else if(reason.equals("wrongSpecialization")) {
            medicalService.setRequiredSpecialization(Specialization.builder().specializationId(UUID.randomUUID()).build()); expected=ShiftUnavailableReason.DEPARTMENT_UNAVAILABLE;
        } else {
            ServiceCapability capability=ServiceCapability.builder().capabilityId(UUID.randomUUID()).active(false).build();
            medicalService.setRequiredCapability(capability);
            if(reason.equals("inactiveCapability")) department.getCapabilities().add(capability);
            expected=ShiftUnavailableReason.CAPABILITY_UNAVAILABLE;
        }
        assertEquals(expected,service.evaluate(medicalService,date,shift,false).reason()); verifyNoInteractions(scheduleRepository);
    }

    @ParameterizedTest @ValueSource(strings={"noProfile","noAccount","inactive","wrongRoom","noRoom","nurse","noRole","wrongSpecialization","excluded"})
    void onlyQualifiedActiveAssignedStaffCanCoverExamination(String reason) {
        Specialization specialty = Specialization.builder().specializationId(UUID.randomUUID()).build();
        department.setSpecialization(specialty); staff.setSpecialization(specialty);
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID()).status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.EXAMINATION).department(department).requiredSpecialization(specialty).build();
        switch(reason) {
            case "noProfile" -> staff.setProfile(null);
            case "noAccount" -> staff.getProfile().setAccount(null);
            case "inactive" -> staff.getProfile().getAccount().setIsActive(false);
            case "wrongRoom" -> staff.setDepartment(Department.builder().departmentId(UUID.randomUUID()).build());
            case "noRoom" -> staff.setDepartment(null);
            case "nurse" -> staff.setSystemRole(SystemRole.NURSE);
            case "noRole" -> staff.setSystemRole(null);
            case "wrongSpecialization" -> staff.setSpecialization(null);
        }
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(date,shift.getShiftId(),ScheduleStatus.SCHEDULED)).thenReturn(List.of(StaffSchedule.builder().staff(staff).build()));
        var result=service.evaluate(medicalService,date,shift,false,reason.equals("excluded")?staff.getStaffId():null);
        assertFalse(result.available()); assertEquals(ShiftUnavailableReason.NO_QUALIFIED_STAFF,result.reason()); assertTrue(result.eligibleStaff().isEmpty());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void unassignedServiceResolvesSpecialtyRooms(boolean roomAvailable) {
        Specialization specialty = Specialization.builder().specializationId(UUID.randomUUID()).build(); staff.setSpecialization(specialty);
        MedicalService medicalService=MedicalService.builder().serviceId(UUID.randomUUID()).status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.EXAMINATION).requiredSpecialization(specialty).build();
        when(departmentRepository.findEligibleExaminationRoomsBySpecialization(specialty.getSpecializationId())).thenReturn(List.of(department));
        if(!roomAvailable) department.setStatus(null);
        else when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(date,shift.getShiftId(),ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build(),StaffSchedule.builder().staff(staff).build()));
        var result=service.evaluate(medicalService,date,shift,false);
        assertEquals(roomAvailable,result.available()); assertEquals(roomAvailable?1:0,result.eligibleStaff().size());
    }

    @Test void closedShiftPreservesReasonAndDoesNotLoadOtherData() {
        when(shiftResolver.resolve(shift,date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(shift,null,null,null,ShiftTimeSource.NORMAL,ShiftUnavailableReason.CLINIC_CLOSED));
        assertEquals(ShiftUnavailableReason.CLINIC_CLOSED,service.evaluate(new MedicalService(),date,shift,false).reason());
        verifyNoInteractions(scheduleRepository,capabilityRepository,departmentRepository);
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

    @Test
    void unassignedCapabilityUsesOnlyAvailableEligibleRoomsAndPermanentCapability() {
        ServiceCapability capability = ServiceCapability.builder().capabilityId(UUID.randomUUID())
                .name("Siêu âm").active(true).build();
        Department unavailable = Department.builder().departmentId(UUID.randomUUID())
                .status(DepartmentStatus.MAINTENANCE).build();
        department.setDepartmentType(DepartmentType.PARACLINICAL);
        department.getCapabilities().add(capability);
        staff.setDepartment(department);
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("IMG-01").name("Siêu âm").status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.PARACLINICAL).requiredCapability(capability).build();
        when(departmentRepository.findEligibleByCapability(capability.getCapabilityId()))
                .thenReturn(List.of(unavailable, department));
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(
                date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build()));
        when(capabilityRepository.findAllByStaff_StaffIdAndStatus(
                staff.getStaffId(), StaffCapabilityStatus.ACTIVE))
                .thenReturn(List.of(StaffCapability.builder().capability(capability)
                        .status(StaffCapabilityStatus.ACTIVE).expiryDate(null).build()));

        var result = service.evaluate(medicalService, date, shift, false);

        assertTrue(result.available());
        assertEquals(List.of(staff), result.eligibleStaff());
    }

    @Test
    void paraclinicalServiceWithoutCapabilityAcceptsAnyActiveScheduledStaff() {
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("OTHER-1").name("Dịch vụ khác").status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.PARACLINICAL).build();
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(
                date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build()));

        assertTrue(service.evaluate(medicalService, date, shift, false).available());
        verifyNoInteractions(capabilityRepository);
    }

    @Test
    void coverageReportsMissingShiftAndUsesStaffCodeWhenNameIsBlank() {
        UUID missingShiftId = UUID.randomUUID();
        MedicalService first = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("S-1").name("Một").status(ServiceStatus.ACTIVE).build();
        MedicalService second = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("S-2").name("Hai").status(ServiceStatus.ACTIVE).build();
        when(shiftRepository.findById(missingShiftId)).thenReturn(Optional.empty());
        when(medicalServiceRepository.findAllByStatus(ServiceStatus.ACTIVE)).thenReturn(List.of(first, second));

        var result = service.coverage(date, missingShiftId);

        assertEquals(2, result.totalActiveServices());
        assertEquals(0, result.coveredServices());
        assertEquals(2, result.uncoveredServices());
        assertTrue(result.services().stream().allMatch(item -> item.reason() == ShiftUnavailableReason.SHIFT_OFF));
    }

    @Test
    void coverageUsesProfileNameOrStaffCodeForEligibleStaff() {
        MedicalService medicalService = MedicalService.builder().serviceId(UUID.randomUUID())
                .serviceCode("OTHER-2").name("Thủ thuật").status(ServiceStatus.ACTIVE)
                .departmentType(DepartmentType.PARACLINICAL).build();
        StaffInfo blankName = StaffInfo.builder().staffId(UUID.randomUUID()).staffCode("STF-FALLBACK")
                .systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().fullName(" ").account(Account.builder().isActive(true).build()).build())
                .build();
        when(shiftRepository.findById(shift.getShiftId())).thenReturn(Optional.of(shift));
        when(medicalServiceRepository.findAllByStatus(ServiceStatus.ACTIVE)).thenReturn(List.of(medicalService));
        when(scheduleRepository.findAllByWorkDateAndShift_ShiftIdAndStatus(
                date, shift.getShiftId(), ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(StaffSchedule.builder().staff(staff).build(),
                        StaffSchedule.builder().staff(blankName).build()));

        var result = service.coverage(date, shift.getShiftId());

        assertEquals(1, result.coveredServices());
        assertEquals(List.of("Doctor A", "STF-FALLBACK"), result.services().get(0).eligibleStaffNames());
    }
}
