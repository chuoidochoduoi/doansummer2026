package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;

import java.util.UUID;

/**
 * Response cho item staff trong lich truc.
 */
public record StaffScheduleItemResponse(
        UUID scheduleId,
        UUID staffId,
        String name,
        String role
) {
    public static StaffScheduleItemResponse from(StaffSchedule schedule) {
        String role = schedule.getStaff() != null ? getRoleCode(schedule.getStaff().getSystemRole()) : null;
        String name = schedule.getStaff() != null
                && schedule.getStaff().getProfile() != null
                ? schedule.getStaff().getProfile().getFullName() : null;
        return new StaffScheduleItemResponse(
                schedule.getScheduleId(),
                schedule.getStaff() != null ? schedule.getStaff().getStaffId() : null,
                name, role);
    }

    public static StaffScheduleItemResponse fromStaff(StaffInfo staff) {
        String role = getRoleCode(staff.getSystemRole());
        String name = staff.getProfile() != null ? staff.getProfile().getFullName() : null;
        return new StaffScheduleItemResponse(null, staff.getStaffId(), name, role);
    }

    private static String getRoleCode(org.example.doansummer2026.enums.SystemRole systemRole) {
        return switch (systemRole) {
            case GENERAL_DOCTOR -> "BS";
            case SPECIALIST_DOCTOR -> "BS";
            case NURSE -> "YT";
            case RECEPTIONIST -> "LT";
            case CASHIER -> "TN";
            case ADMIN -> "AD";
            case CLINIC_MANAGER -> "QL";
        };
    }
}