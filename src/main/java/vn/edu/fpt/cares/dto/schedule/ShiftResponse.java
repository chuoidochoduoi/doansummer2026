package vn.edu.fpt.cares.dto.schedule;

import vn.edu.fpt.cares.model.ShiftConfig;
import vn.edu.fpt.cares.model.StaffSchedule;

import java.util.UUID;

/**
 * Response cho ca truc (shift).
 */
public record ShiftResponse(
        String id,
        String name,
        String startTime,
        String endTime
) {
    public static ShiftResponse from(ShiftConfig shift) {
        return new ShiftResponse(
                shift.getShiftId().toString(),
                shift.getName(),
                shift.getStartTime(),
                shift.getEndTime()
        );
    }

    public static ShiftResponse from(StaffSchedule schedule) {
        if (schedule == null || schedule.getShift() == null) return null;
        String start = schedule.getActualStartTime() == null ? schedule.getShift().getStartTime()
                : schedule.getActualStartTime().toString();
        String end = schedule.getActualEndTime() == null ? schedule.getShift().getEndTime()
                : schedule.getActualEndTime().toString();
        return new ShiftResponse(schedule.getShift().getShiftId().toString(),
                schedule.getShift().getName(), start, end);
    }
}
