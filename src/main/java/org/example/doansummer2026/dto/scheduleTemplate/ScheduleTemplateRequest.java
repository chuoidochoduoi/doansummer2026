package org.example.doansummer2026.dto.scheduleTemplate;

import jakarta.validation.constraints.NotNull;
import org.example.doansummer2026.enums.Shift;

import java.time.DayOfWeek;
import java.util.UUID;

public record ScheduleTemplateRequest(
        @NotNull UUID staffId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull Shift shift,
        Boolean isActive
) {}