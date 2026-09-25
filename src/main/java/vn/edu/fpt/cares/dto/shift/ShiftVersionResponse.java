package vn.edu.fpt.cares.dto.shift;

import vn.edu.fpt.cares.model.ShiftVersion;
import java.time.*;
import java.util.UUID;

public record ShiftVersionResponse(
        UUID shiftVersionId, UUID shiftId, String shiftName,
        LocalTime startTime, LocalTime endTime,
        LocalDate effectiveFrom, LocalDate effectiveTo,
        String changeReason, LocalDateTime createdAt
) {
    public static ShiftVersionResponse from(ShiftVersion value) {
        return new ShiftVersionResponse(value.getShiftVersionId(), value.getShift().getShiftId(),
                value.getShift().getName(), value.getStartTime(), value.getEndTime(),
                value.getEffectiveFrom(), value.getEffectiveTo(), value.getChangeReason(),
                value.getCreatedAt());
    }
}
