package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.schedule.ClinicManagerScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleAssignRequest;
import org.example.doansummer2026.dto.schedule.ScheduleCopyRequest;
import org.example.doansummer2026.dto.schedule.ScheduleCreateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleGenerateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleResponse;
import org.example.doansummer2026.dto.schedule.ScheduleShiftUpdateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleUpdateRequest;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.service.StaffScheduleService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StaffScheduleController {

    private final StaffScheduleService service;
    private final ShiftConfigRepository shiftConfigRepo;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/schedules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<PageResponse<ScheduleResponse>> search(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID shiftId,
            Pageable pageable) {
        return RestResponses.ok(service.search(staffId, from, to, shiftId, pageable));
    }

    @GetMapping("/api/v1/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ScheduleResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping("/api/v1/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "StaffSchedule")
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleCreateRequest req) {
        ScheduleResponse created = service.create(req);
        return RestResponses.created("/api/v1/schedules/{id}", created.scheduleId(), created);
    }

    @PutMapping("/api/v1/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "StaffSchedule", idParamName = "id")
    public ResponseEntity<ScheduleResponse> update(@PathVariable UUID id,
                                                   @RequestBody ScheduleUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/api/v1/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "StaffSchedule", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /** POST tac vu batch - sinh nhieu lich, khong co Location don le -> 200 OK. */
    @PostMapping("/api/v1/schedules/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "StaffSchedule")
    public ResponseEntity<List<ScheduleResponse>> generate(@RequestBody ScheduleGenerateRequest req) {
        return RestResponses.ok(service.generateFromTemplates(
                req.weekStart(), req.staffIds(), req.overrideExisting()));
    }

    // --- CLINIC MANAGER ENDPOINTS ---

    /**
     * API lay lich truc cho Clinic Manager.
     * - week: ngay bat ky trong tuan (thu 2 - chu nhat).
     */
    @GetMapping("/api/v1/clinic-manager/schedules")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ClinicManagerScheduleResponse> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        LocalDate weekStart = week.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        var schedules = service.findByWeek(weekStart, weekEnd);
        var response = ClinicManagerScheduleResponse.from(schedules, weekStart, shiftConfigRepo.findAll());
        return RestResponses.ok(response);
    }

    /**
     * Gán nhân sự vào ca truc.
     * - action: add hoặc remove
     */
    @PostMapping("/api/v1/clinic-manager/schedules/assign")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "StaffSchedule")
    public ResponseEntity<Void> assign(@Valid @RequestBody ScheduleAssignRequest req) {
        service.assignStaff(req);
        return RestResponses.noContent();
    }

    /**
     * Sao chep lich sang tuan moi.
     */
    @PostMapping("/api/v1/clinic-manager/schedules/copy")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.CREATE, entityName = "StaffSchedule")
    public ResponseEntity<ClinicManagerScheduleResponse> copy(@Valid @RequestBody ScheduleCopyRequest req) {
        LocalDate weekStart = req.week().with(DayOfWeek.MONDAY);
        LocalDate prevWeekStart = weekStart.minusDays(7);
        LocalDate weekEnd = weekStart.plusDays(6);

        var schedules = service.copyWeek(prevWeekStart, weekStart);
        var response = ClinicManagerScheduleResponse.from(schedules, weekStart, shiftConfigRepo.findAll());
        return RestResponses.ok(response);
    }

    /**
     * Luu ca truc (shift template).
     */
    @PutMapping("/api/v1/clinic-manager/schedules/shifts")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> updateShifts(@Valid @RequestBody ScheduleShiftUpdateRequest req) {
        // Hien tai chi co 3 shift co ban, khong cho sua
        // Neu can them shift moi, sua logic o day
        return RestResponses.noContent();
    }
}
