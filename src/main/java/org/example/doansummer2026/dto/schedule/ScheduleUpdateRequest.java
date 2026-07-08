package org.example.doansummer2026.dto.schedule;

import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.Shift;

import java.util.UUID;

public record ScheduleUpdateRequest(
        Shift shift,
        ScheduleStatus status,
        Boolean isCustom,
        String note
) {}