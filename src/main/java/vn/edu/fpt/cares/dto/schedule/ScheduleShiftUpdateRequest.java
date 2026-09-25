package vn.edu.fpt.cares.dto.schedule;

import jakarta.validation.constraints.NotEmpty;
import vn.edu.fpt.cares.enums.Shift;

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