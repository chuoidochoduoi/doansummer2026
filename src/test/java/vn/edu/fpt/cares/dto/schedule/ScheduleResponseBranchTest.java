package vn.edu.fpt.cares.dto.schedule;

import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleResponseBranchTest {

    @ParameterizedTest
    @EnumSource(SystemRole.class)
    void staffItemMapsEveryRoleAndOptionalAssociations(SystemRole role) {
        Department room = Department.builder().departmentId(UUID.randomUUID()).name("Phòng khám").build();
        StaffInfo staff = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(role)
                .profile(Profile.builder().fullName("Nhân viên").build()).department(room).build();
        StaffScheduleItemResponse response = StaffScheduleItemResponse.fromStaff(staff);
        assertNotNull(response.role());
        assertEquals("Nhân viên", response.name());
        assertEquals(room.getDepartmentId(), response.departmentId());

        staff.setProfile(null);
        staff.setDepartment(null);
        response = StaffScheduleItemResponse.fromStaff(staff);
        assertNull(response.name());
        assertNull(response.departmentId());
        assertNull(response.departmentName());
    }

    @Test
    void scheduleItemHandlesMissingStaffProfileAndDepartment() {
        StaffSchedule empty = StaffSchedule.builder().scheduleId(UUID.randomUUID()).build();
        StaffScheduleItemResponse response = StaffScheduleItemResponse.from(empty);
        assertNull(response.staffId());
        assertNull(response.role());
        assertNull(response.name());
        assertNull(response.departmentId());

        StaffInfo staff = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.NURSE).build();
        empty.setStaff(staff);
        response = StaffScheduleItemResponse.from(empty);
        assertEquals(staff.getStaffId(), response.staffId());
        assertNull(response.name());
        assertNull(response.departmentName());
    }

    @Test
    void myScheduleOnlyIncludesMatchingStaffAndInitializesEveryCell() {
        UUID staffId = UUID.randomUUID();
        ShiftConfig shift = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Ca sáng")
                .startTime("07:30").endTime("11:30").build();
        StaffInfo mine = StaffInfo.builder().staffId(staffId).systemRole(SystemRole.DOCTOR)
                .profile(Profile.builder().fullName("Bác sĩ").build()).build();
        StaffInfo other = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR).build();
        LocalDate monday = LocalDate.of(2026, 9, 7);
        StaffSchedule matching = StaffSchedule.builder().scheduleId(UUID.randomUUID()).staff(mine)
                .workDate(monday).shift(shift).build();
        StaffSchedule ignoredOther = StaffSchedule.builder().scheduleId(UUID.randomUUID()).staff(other)
                .workDate(monday).shift(shift).build();
        StaffSchedule ignoredNull = StaffSchedule.builder().scheduleId(UUID.randomUUID()).staff(null)
                .workDate(monday).shift(shift).build();

        MyScheduleResponse response = MyScheduleResponse.from(
                List.of(matching, ignoredOther, ignoredNull), staffId, List.of(shift));
        assertEquals(7, response.schedule().size());
        assertEquals(1, response.schedule().get(MyScheduleResponse.toKey(
                shift.getShiftId().toString(), DayOfWeek.MONDAY)).size());
        assertEquals(1, response.shifts().size());

        MyScheduleResponse noOwner = MyScheduleResponse.from(List.of(matching), null, List.of(shift));
        assertTrue(noOwner.schedule().values().stream().allMatch(List::isEmpty));
    }

    @Test
    void shiftResponseUsesDefaultsOverridesAndNullGuard() {
        ShiftConfig shift = ShiftConfig.builder().shiftId(UUID.randomUUID()).name("Ca sáng")
                .startTime("07:30").endTime("11:30").build();
        StaffSchedule schedule = StaffSchedule.builder().shift(shift).build();
        assertEquals("07:30", ShiftResponse.from(schedule).startTime());
        assertEquals("11:30", ShiftResponse.from(schedule).endTime());
        schedule.setActualStartTime(java.time.LocalTime.of(8, 0));
        schedule.setActualEndTime(java.time.LocalTime.NOON);
        assertEquals("08:00", ShiftResponse.from(schedule).startTime());
        assertEquals("12:00", ShiftResponse.from(schedule).endTime());
        assertNull(ShiftResponse.from((StaffSchedule) null));
        assertNull(ShiftResponse.from(StaffSchedule.builder().build()));
    }
}
