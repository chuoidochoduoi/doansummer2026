package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.model.ShiftConfig;

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
}