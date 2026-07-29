package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.schedule.ClinicManagerScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleAssignRequest;
import org.example.doansummer2026.dto.schedule.ScheduleCopyRequest;
import org.example.doansummer2026.dto.schedule.ScheduleShiftUpdateRequest;
import org.example.doansummer2026.enums.Shift;
import org.example.doansummer2026.service.StaffScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinic-manager/schedules")
@RequiredArgsConstructor
public class ClinicManagerScheduleController {

    private final StaffScheduleService staffScheduleService;

    /**
     * API lay lich truc cho Clinic Manager.
     * - week: ngay bat ky trong tuan (thu 2 - chu nhat).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ClinicManagerScheduleResponse> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        LocalDate weekStart = week.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        var schedules = staffScheduleService.findByWeek(weekStart, weekEnd);
        var response = ClinicManagerScheduleResponse.from(schedules, weekStart);
        return RestResponses.ok(response);
    }

    /**
     * Gán nhân sự vào ca truc.
     * - action: add hoặc remove
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<Void> assign(@Valid @RequestBody ScheduleAssignRequest req) {
        staffScheduleService.assignStaff(req);
        return RestResponses.noContent();
    }

    /**
     * Sao chep lich sang tuan moi.
     */
    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ClinicManagerScheduleResponse> copy(@Valid @RequestBody ScheduleCopyRequest req) {
        LocalDate weekStart = req.week().with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusDays(7);
        LocalDate weekEnd = weekStart.plusDays(6);

        var schedules = staffScheduleService.copyWeek(prevWeekStart, weekStart);
        var response = ClinicManagerScheduleResponse.from(schedules, weekStart);
        return RestResponses.ok(response);
    }

    /**
     * Luu ca truc (shift template).
     */
    @PutMapping("/shifts")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<Void> updateShifts(@Valid @RequestBody ScheduleShiftUpdateRequest req) {
        // Hien tai chi co 3 shift co ban, khong cho sua
        // Neu can them shift moi, sua logic o day
        return RestResponses.noContent();
    }
}