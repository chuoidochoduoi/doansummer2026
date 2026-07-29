package org.example.doansummer2026.dto.schedule;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request sao chep lich tu tuan truoc.
 */
public record ScheduleCopyRequest(
        @NotNull LocalDate week
) {}