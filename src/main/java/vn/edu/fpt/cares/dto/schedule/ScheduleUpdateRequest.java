package vn.edu.fpt.cares.dto.schedule;

import vn.edu.fpt.cares.enums.ScheduleStatus;

import java.util.UUID;

public record ScheduleUpdateRequest(
        UUID shiftId,
        ScheduleStatus status,
        Boolean isCustom,
        String note
) {}



