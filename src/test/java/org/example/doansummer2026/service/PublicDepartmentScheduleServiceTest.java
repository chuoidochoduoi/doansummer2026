package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.ClinicScheduleExceptionType;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.ClinicScheduleException;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.Specialization;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.repository.ClinicScheduleExceptionRepository;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicDepartmentScheduleServiceTest {

    @Mock StaffScheduleRepository scheduleRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock ShiftConfigRepository shiftRepository;
    @Mock ClinicScheduleExceptionRepository exceptionRepository;
    @InjectMocks PublicDepartmentScheduleService service;

    @Test
    void getWeekGroupsClinicalAndParaclinicalRoomsAndAppliesClosures() {
        LocalDate requested = LocalDate.of(2026, 9, 3);
        LocalDate monday = LocalDate.of(2026, 8, 31);
        LocalDate sunday = LocalDate.of(2026, 9, 6);
        ShiftConfig morning = shift("Sáng", "08:00", "12:00");
        ShiftConfig afternoon = shift("Chiều", "13:00", "17:00");
        Specialization internal = Specialization.builder().specializationId(UUID.randomUUID())
                .name("Nội khoa").active(true).build();
        Department clinical = department("INT-101", "Phòng Nội", DepartmentType.EXAMINATION,
                DepartmentStatus.AVAILABLE, internal);
        Department clinicalNoType = department("INT-102", "Phòng Nội 2", null,
                DepartmentStatus.IN_SESSION, internal);
        Department laboratory = department("LAB-201", "Xét nghiệm", DepartmentType.LABORATORY,
                DepartmentStatus.AVAILABLE, null);
        Department hiddenInactive = department("OFF-101", "Khoa đóng", DepartmentType.EXAMINATION,
                DepartmentStatus.AVAILABLE, Specialization.builder().specializationId(UUID.randomUUID())
                        .name("Đã đóng").active(false).build());
        Department hiddenNoSpecialization = department("NO-101", "Thiếu khoa", DepartmentType.EXAMINATION,
                DepartmentStatus.AVAILABLE, null);
        Department maintenance = department("IMG-301", "Bảo trì", DepartmentType.IMAGING,
                DepartmentStatus.MAINTENANCE, null);

        List<StaffSchedule> schedules = List.of(
                schedule(clinical, SystemRole.DOCTOR, monday, morning, ScheduleStatus.SCHEDULED),
                schedule(clinicalNoType, SystemRole.NURSE, monday, morning, ScheduleStatus.SCHEDULED),
                schedule(laboratory, SystemRole.NURSE, monday, afternoon, ScheduleStatus.SCHEDULED),
                schedule(laboratory, SystemRole.DOCTOR, monday.plusDays(1), morning, ScheduleStatus.SCHEDULED),
                schedule(clinical, SystemRole.DOCTOR, monday.plusDays(1), morning, ScheduleStatus.ABSENT),
                StaffSchedule.builder().workDate(monday).shift(morning).status(ScheduleStatus.SCHEDULED).build(),
                StaffSchedule.builder().staff(StaffInfo.builder().build()).workDate(monday)
                        .shift(morning).status(ScheduleStatus.SCHEDULED).build(),
                schedule(null, SystemRole.DOCTOR, monday, morning, ScheduleStatus.SCHEDULED),
                schedule(clinical, SystemRole.RECEPTIONIST, monday, morning, ScheduleStatus.SCHEDULED),
                StaffSchedule.builder().staff(staff(clinical, SystemRole.DOCTOR)).workDate(monday)
                        .status(ScheduleStatus.SCHEDULED).build());

        List<ClinicScheduleException> exceptions = List.of(
                exception(monday, null, ClinicScheduleExceptionType.CLOSED_DAY),
                exception(monday.plusDays(1), morning, ClinicScheduleExceptionType.SHIFT_OFF),
                exception(monday.plusDays(2), null, ClinicScheduleExceptionType.SHIFT_OFF),
                exception(monday.plusDays(3), afternoon, ClinicScheduleExceptionType.SPECIAL_HOURS));

        when(shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc())
                .thenReturn(List.of(morning, afternoon));
        when(departmentRepository.findAll()).thenReturn(List.of(laboratory, clinicalNoType,
                hiddenInactive, clinical, hiddenNoSpecialization, maintenance));
        when(scheduleRepository.findAllByWorkDateBetween(monday, sunday)).thenReturn(schedules);
        when(exceptionRepository.findAllByWorkDateBetweenOrderByWorkDateAsc(monday, sunday))
                .thenReturn(exceptions);

        var response = service.getWeek(requested);

        assertAll(
                () -> assertEquals(monday, response.weekStart()),
                () -> assertEquals(sunday, response.weekEnd()),
                () -> assertEquals(2, response.shifts().size()),
                () -> assertEquals("Sáng", response.shifts().get(0).name()),
                () -> assertEquals(2, response.departments().size()),
                () -> assertEquals("Nội khoa", response.departments().get(0).groupName()),
                () -> assertEquals("CLINICAL", response.departments().get(0).groupType()),
                () -> assertEquals("Cận lâm sàng", response.departments().get(1).groupName()),
                () -> assertEquals("PARACLINICAL", response.departments().get(1).groupType()),
                () -> assertEquals(14, response.departments().get(0).availability().size()),
                () -> assertFalse(available(response, "CLINICAL", monday, morning)),
                () -> assertFalse(available(response, "PARACLINICAL", monday, afternoon)),
                () -> assertFalse(available(response, "PARACLINICAL", monday.plusDays(1), morning)));
    }

    @Test
    void getWeekMarksScheduledDoctorAndParaclinicalNurseAvailableWithoutExceptions() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        ShiftConfig shift = shift("Sáng", "08:00", "12:00");
        Specialization specialization = Specialization.builder().specializationId(UUID.randomUUID())
                .name("Ngoại khoa").active(true).build();
        Department clinical = department("SUR-101", "Ngoại", DepartmentType.EXAMINATION,
                DepartmentStatus.AVAILABLE, specialization);
        Department imaging = department("IMG-101", "Siêu âm", DepartmentType.PARACLINICAL,
                DepartmentStatus.AVAILABLE, null);
        when(shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        when(departmentRepository.findAll()).thenReturn(List.of(clinical, imaging));
        when(scheduleRepository.findAllByWorkDateBetween(monday, monday.plusDays(6))).thenReturn(List.of(
                schedule(clinical, SystemRole.DOCTOR, monday, shift, ScheduleStatus.SCHEDULED),
                schedule(imaging, SystemRole.NURSE, monday.plusDays(1), shift, ScheduleStatus.SCHEDULED)));
        when(exceptionRepository.findAllByWorkDateBetweenOrderByWorkDateAsc(monday, monday.plusDays(6)))
                .thenReturn(List.of());

        var response = service.getWeek(monday);

        assertTrue(available(response, "CLINICAL", monday, shift));
        assertTrue(available(response, "PARACLINICAL", monday.plusDays(1), shift));
        assertFalse(available(response, "CLINICAL", monday.plusDays(1), shift));
    }

    @Test
    void getWeekReturnsEmptyGroupsWhenNoEligibleDepartmentExists() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        when(shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of());
        when(departmentRepository.findAll()).thenReturn(List.of(
                department("X", "Không cấu hình", DepartmentType.EXAMINATION,
                        DepartmentStatus.AVAILABLE, null)));
        when(scheduleRepository.findAllByWorkDateBetween(monday, monday.plusDays(6))).thenReturn(List.of());
        when(exceptionRepository.findAllByWorkDateBetweenOrderByWorkDateAsc(monday, monday.plusDays(6)))
                .thenReturn(List.of());

        var response = service.getWeek(monday);

        assertTrue(response.shifts().isEmpty());
        assertTrue(response.departments().isEmpty());
    }

    private boolean available(org.example.doansummer2026.dto.schedule.PublicDepartmentScheduleResponse response,
                              String type, LocalDate date, ShiftConfig shift) {
        return response.departments().stream().filter(group -> type.equals(group.groupType())).findFirst()
                .orElseThrow().availability().stream()
                .filter(item -> date.equals(item.date()) && shift.getShiftId().equals(item.shiftId()))
                .findFirst().orElseThrow().available();
    }

    private ShiftConfig shift(String name, String start, String end) {
        return ShiftConfig.builder().shiftId(UUID.randomUUID()).name(name)
                .startTime(start).endTime(end).isActive(true).build();
    }

    private Department department(String code, String name, DepartmentType type,
                                  DepartmentStatus status, Specialization specialization) {
        return Department.builder().departmentId(UUID.randomUUID()).roomCode(code).name(name)
                .departmentType(type).status(status).specialization(specialization).build();
    }

    private StaffInfo staff(Department department, SystemRole role) {
        return StaffInfo.builder().staffId(UUID.randomUUID()).department(department).systemRole(role).build();
    }

    private StaffSchedule schedule(Department department, SystemRole role, LocalDate date,
                                   ShiftConfig shift, ScheduleStatus status) {
        return StaffSchedule.builder().staff(staff(department, role)).workDate(date)
                .shift(shift).status(status).build();
    }

    private ClinicScheduleException exception(LocalDate date, ShiftConfig shift,
                                              ClinicScheduleExceptionType type) {
        return ClinicScheduleException.builder().workDate(date).shift(shift).type(type).reason("Demo").build();
    }
}
