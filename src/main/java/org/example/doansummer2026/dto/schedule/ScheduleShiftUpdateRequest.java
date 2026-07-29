package org.example.doansummer2026.dto.schedule;

import jakarta.validation.constraints.NotEmpty;
import org.example.doansummer2026.enums.Shift;

import java.util.List;

/**
 * Request luu ca truc.
 */
public record ScheduleShiftUpdateRequest(
        @NotEmpty List<ShiftItem> shifts
) {
    public record ShiftItem(
            String id,
            String name,
            String startTime,
            String endTime
    ) {}
}