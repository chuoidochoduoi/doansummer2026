package org.example.doansummer2026.dto.schedule;

import jakarta.validation.constraints.NotNull;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.Shift;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleCreateRequest(
        @NotNull UUID staffId,
        @NotNull LocalDate workDate,
        @NotNull Shift shift,
        ScheduleStatus status,
        Boolean isCustom,
        UUID templateId,
        String note
) {}



