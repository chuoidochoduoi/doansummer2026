package org.example.doansummer2026.dto.schedule;

import jakarta.validation.constraints.NotNull;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleCreateRequest(
        @NotNull UUID staffId,
        @NotNull LocalDate workDate,
        @NotNull UUID shiftId,
        ScheduleStatus status,
        Boolean isCustom,
        UUID templateId,
        String note
) {}



