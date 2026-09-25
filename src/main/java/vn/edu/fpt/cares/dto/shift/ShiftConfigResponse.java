package vn.edu.fpt.cares.dto.shift;

import vn.edu.fpt.cares.model.ShiftConfig;

import java.util.UUID;

public record ShiftConfigResponse(
        UUID shiftId,
        String name,
        String startTime,
        String endTime,
        Boolean isActive
) {
    public static ShiftConfigResponse from(ShiftConfig shift) {
        return new ShiftConfigResponse(
                shift.getShiftId(),
                shift.getName(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getIsActive()
        );
    }
}
