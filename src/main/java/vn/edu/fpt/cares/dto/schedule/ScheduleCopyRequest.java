package vn.edu.fpt.cares.dto.schedule;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request sao chep lich tu tuan truoc.
 */
public record ScheduleCopyRequest(
        @NotNull LocalDate week
) {}