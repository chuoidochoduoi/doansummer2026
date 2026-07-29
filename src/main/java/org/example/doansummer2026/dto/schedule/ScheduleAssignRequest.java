package org.example.doansummer2026.dto.schedule;

import jakarta.validation.constraints.NotNull;
import org.example.doansummer2026.enums.Shift;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request gán nhân sự vào ca truc.
 */
public record ScheduleAssignRequest(
        @NotNull LocalDate week,
        @NotNull String shiftId,  // morning, afternoon, evening
        @NotNull String dayKey,   // mon, tue, wed, thu, fri, sat, sun
        @NotNull UUID staffId,
        @NotNull String action   // add hoac remove
) {}