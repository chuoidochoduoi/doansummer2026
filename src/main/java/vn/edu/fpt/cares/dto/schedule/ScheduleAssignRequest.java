package vn.edu.fpt.cares.dto.schedule;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request gán nhân sự vào ca truc.
 */
public record ScheduleAssignRequest(
        @NotNull LocalDate week,
        @NotNull UUID shiftId,  // UUID của ShiftConfig
        @NotNull String dayKey,   // mon, tue, wed, thu, fri, sat, sun
        @NotNull UUID staffId,
        @NotNull String action   // add hoac remove
) {}
