package vn.edu.fpt.cares.dto.schedule;

import vn.edu.fpt.cares.enums.Shift;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.StaffSchedule;

import java.util.UUID;

/**
 * Response cho item staff trong lich truc.
 */
public record StaffScheduleItemResponse(
        UUID scheduleId,
        UUID staffId,
        String name,
        String role,
        UUID departmentId,
        String departmentName
) {
    public static StaffScheduleItemResponse from(StaffSchedule schedule) {
        String role = schedule.getStaff() != null ? getRoleCode(schedule.getStaff().getSystemRole()) : null;
        String name = schedule.getStaff() != null
                && schedule.getStaff().getProfile() != null
                ? schedule.getStaff().getProfile().getFullName() : null;
        return new StaffScheduleItemResponse(
                schedule.getScheduleId(),
                schedule.getStaff() != null ? schedule.getStaff().getStaffId() : null,
                name, role,
                schedule.getStaff() != null && schedule.getStaff().getDepartment() != null
                        ? schedule.getStaff().getDepartment().getDepartmentId() : null,
                schedule.getStaff() != null && schedule.getStaff().getDepartment() != null
                        ? schedule.getStaff().getDepartment().getName() : null);
    }

    public static StaffScheduleItemResponse fromStaff(StaffInfo staff) {
        String role = getRoleCode(staff.getSystemRole());
        String name = staff.getProfile() != null ? staff.getProfile().getFullName() : null;
        return new StaffScheduleItemResponse(null, staff.getStaffId(), name, role,
                staff.getDepartment() != null ? staff.getDepartment().getDepartmentId() : null,
                staff.getDepartment() != null ? staff.getDepartment().getName() : null);
    }

    private static String getRoleCode(vn.edu.fpt.cares.enums.SystemRole systemRole) {
        return switch (systemRole) {
            case DOCTOR, GENERAL_DOCTOR, SPECIALIST_DOCTOR -> "BS";
            case NURSE -> "YT";
            case RECEPTIONIST -> "LT";
            case CASHIER -> "TN";
            case ADMIN -> "AD";
            case CLINIC_MANAGER -> "QL";
        };
    }
}
