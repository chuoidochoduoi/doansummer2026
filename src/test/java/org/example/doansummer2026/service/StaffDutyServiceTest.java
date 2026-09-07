package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffDutyServiceTest {
    @Mock StaffScheduleRepository schedules;
    @Mock StaffInfoRepository staffRepository;
    @Mock StaffCapabilityRepository capabilities;
    @Mock AuthService auth;
    @InjectMocks StaffDutyService service;

    private final LocalDate date = LocalDate.of(2030, 6, 10);
    private final Department room = Department.builder().departmentId(UUID.randomUUID())
            .departmentType(DepartmentType.EXAMINATION).build();
    private final StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID())
            .systemRole(SystemRole.DOCTOR).department(room).build();

    private StaffSchedule schedule(String start, String end) {
        return StaffSchedule.builder().staff(doctor).status(ScheduleStatus.SCHEDULED)
                .workDate(date).actualStartTime(LocalTime.parse(start))
                .actualEndTime(LocalTime.parse(end)).build();
    }

    @Test
    void absentActorCannotOperate() {
        assertThrows(BadRequestException.class, () -> service.requireCurrentStaffOnDuty(room, true));
        verifyNoInteractions(staffRepository, schedules);
    }

    @Test
    void unknownStaffIsNotFound() {
        when(auth.currentStaffId()).thenReturn(doctor.getStaffId());
        assertThrows(ResourceNotFoundException.class, () -> service.requireCurrentStaffOnDuty(room, true));
        verifyNoInteractions(schedules);
    }

    @Test
    void staffFromAnotherRoomIsRejectedBeforeScheduleLookup() {
        when(auth.currentStaffId()).thenReturn(doctor.getStaffId());
        when(staffRepository.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
        Department otherRoom = Department.builder().departmentId(UUID.randomUUID()).build();
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.requireCurrentStaffOnDuty(otherRoom, true));
        assertEquals("Nhân sự không thuộc phòng thực hiện", error.getMessage());
        verifyNoInteractions(schedules);
    }

    @Test
    void nurseCannotPerformDoctorOnlyAction() {
        doctor.setSystemRole(SystemRole.NURSE);
        when(auth.currentStaffId()).thenReturn(doctor.getStaffId());
        when(staffRepository.findById(doctor.getStaffId())).thenReturn(Optional.of(doctor));
        assertThrows(BadRequestException.class, () -> service.requireCurrentStaffOnDuty(room, true));
        verifyNoInteractions(schedules);
    }

    @Test
    void missingMembershipInputsAreRejected() {
        assertThrows(BadRequestException.class, () -> service.requireMembership(null, room));
        assertThrows(BadRequestException.class, () -> service.requireMembership(doctor, null));
        doctor.setDepartment(null);
        assertThrows(BadRequestException.class, () -> service.requireMembership(doctor, room));
    }

    @Test
    void missingQualificationInputsAreRejected() {
        assertThrows(BadRequestException.class, () -> service.requireEligibility(null, room));
        assertThrows(BadRequestException.class, () -> service.requireEligibility(doctor, null));
        doctor.setSystemRole(null);
        assertThrows(BadRequestException.class, () -> service.requireEligibility(doctor, room));
    }

    @Test
    void examinationDoctorMustMatchRoomSpecialization() {
        Specialization specialty = Specialization.builder().specializationId(UUID.randomUUID()).build();
        room.setSpecialization(specialty);
        assertThrows(BadRequestException.class, () -> service.requireEligibility(doctor, room));
        doctor.setSpecialization(Specialization.builder().specializationId(UUID.randomUUID()).build());
        assertThrows(BadRequestException.class, () -> service.requireEligibility(doctor, room));
        doctor.setSpecialization(specialty);
        assertDoesNotThrow(() -> service.requireEligibility(doctor, room));
        verifyNoInteractions(capabilities);
    }

    @Test
    void labRequiresConfiguredAndActiveCapability() {
        room.setDepartmentType(DepartmentType.PARACLINICAL);
        assertThrows(BadRequestException.class, () -> service.requireEligibility(doctor, room));
        ServiceCapability capability = ServiceCapability.builder().capabilityId(UUID.randomUUID()).build();
        room.getCapabilities().add(capability);
        assertThrows(BadRequestException.class, () -> service.requireEligibility(doctor, room));
        when(capabilities.existsByStaff_StaffIdAndCapability_CapabilityIdAndStatus(
                doctor.getStaffId(), capability.getCapabilityId(), StaffCapabilityStatus.ACTIVE)).thenReturn(true);
        assertDoesNotThrow(() -> service.requireEligibility(doctor, room));
    }

    @Test
    void missingStaffOrTimeIsNotOnDuty() {
        assertFalse(service.isOnDuty(null, date.atStartOfDay()));
        assertFalse(service.isOnDuty(doctor, null));
        verifyNoInteractions(schedules);
    }

    @ParameterizedTest
    @CsvSource({"07:59,false", "08:00,true", "11:59,true", "12:00,false"})
    void normalShiftIncludesStartAndExcludesEnd(String time, boolean expected) {
        when(schedules.findAllByStaff_StaffIdAndWorkDate(doctor.getStaffId(), date))
                .thenReturn(List.of(schedule("08:00", "12:00")));
        assertEquals(expected, service.isOnDuty(doctor, date.atTime(LocalTime.parse(time))));
    }

    @ParameterizedTest
    @CsvSource({"00:00,true", "05:59,true", "06:00,false"})
    void previousDayOvernightShiftIsConsidered(String time, boolean expected) {
        when(schedules.findAllByStaff_StaffIdAndWorkDate(doctor.getStaffId(), date.plusDays(1)))
                .thenReturn(List.of());
        when(schedules.findAllByStaff_StaffIdAndWorkDate(doctor.getStaffId(), date))
                .thenReturn(List.of(schedule("22:00", "06:00")));
        assertEquals(expected, service.isOnDuty(doctor, date.plusDays(1).atTime(LocalTime.parse(time))));
    }

    @Test
    void endOfDaySentinelIncludesFinalSecondButNotNextDay() {
        when(schedules.findAllByStaff_StaffIdAndWorkDate(doctor.getStaffId(), date))
                .thenReturn(List.of(schedule("16:00", "23:59:59")));
        assertTrue(service.isOnDuty(doctor, date.atTime(23, 59, 59)));
        assertFalse(service.isOnDuty(doctor, date.plusDays(1).atStartOfDay()));
    }

    @Test
    void configuredShiftTimesAreUsedWhenActualTimesAbsent() {
        StaffSchedule schedule = StaffSchedule.builder().workDate(date).status(ScheduleStatus.SCHEDULED)
                .shift(ShiftConfig.builder().startTime("08:00").endTime("12:00").build()).build();
        when(schedules.findAllByStaff_StaffIdAndWorkDate(doctor.getStaffId(), date)).thenReturn(List.of(schedule));
        assertTrue(service.isOnDuty(doctor, date.atTime(9, 0)));
    }

    @Test
    void incompleteOrUnscheduledRowsDoNotCountAsDuty() {
        StaffSchedule missingTime = StaffSchedule.builder().workDate(date).status(ScheduleStatus.SCHEDULED).build();
        StaffSchedule missingDate = schedule("08:00", "12:00");
        missingDate.setWorkDate(null);
        StaffSchedule missingStatus = schedule("08:00", "12:00");
        missingStatus.setStatus(null);
        when(schedules.findAllByStaff_StaffIdAndWorkDate(doctor.getStaffId(), date))
                .thenReturn(List.of(missingTime, missingDate, missingStatus));
        assertFalse(service.isOnDuty(doctor, date.atTime(9, 0)));
    }

    @Test
    void onDutyListingDeduplicatesStaffAndExcludesInactiveAccounts() {
        doctor.setProfile(Profile.builder().account(Account.builder().isActive(true).build()).build());
        StaffInfo inactive = StaffInfo.builder().staffId(UUID.randomUUID()).department(room)
                .profile(Profile.builder().account(Account.builder().isActive(false).build()).build()).build();
        StaffSchedule other = schedule("08:00", "12:00");
        other.setStaff(inactive);
        when(schedules.findAllByWorkDateAndStatus(date, ScheduleStatus.SCHEDULED))
                .thenReturn(List.of(schedule("08:00", "12:00"), schedule("08:00", "12:00"), other));
        assertEquals(List.of(doctor), service.findOnDutyStaff(room, date.atTime(9, 0)));
        assertTrue(service.findOnDutyStaff(null, date.atTime(9, 0)).isEmpty());
    }
}
