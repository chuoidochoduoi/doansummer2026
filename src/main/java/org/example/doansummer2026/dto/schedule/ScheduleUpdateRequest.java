package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.ScheduleStatus;

import java.util.UUID;

public record ScheduleUpdateRequest(
        UUID shiftId,
        ScheduleStatus status,
        Boolean isCustom,
        String note
) {}



