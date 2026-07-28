package org.example.doansummer2026.dto.schedule;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Sinh lich cho 1 tuan tu cac template cua 1 hoac nhieu nhan vien.
 * - weekStart: ngay thu 2 cua tuan (LocalDate, dinh dang bat ky).
 * - staffIds: null/rong = sinh cho tat ca staff co template.
 * - overrideExisting: neu true, ghi de cac StaffSchedule cung (staff, workDate, shift) neu da ton tai.
 */
public record ScheduleGenerateRequest(
        @NotNull LocalDate weekStart,
        java.util.List<UUID> staffIds,
        Boolean overrideExisting
) {}



