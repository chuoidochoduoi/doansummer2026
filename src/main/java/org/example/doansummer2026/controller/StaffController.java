package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.schedule.MyScheduleResponse;
import org.example.doansummer2026.dto.staff.StaffCreateRequest;
import org.example.doansummer2026.dto.staff.StaffOptionResponse;
import org.example.doansummer2026.dto.staff.StaffResponse;
import org.example.doansummer2026.dto.staff.StaffUpdateRequest;
import org.example.doansummer2026.dto.staff.StaffProfessionalUpdateRequest;
import org.example.doansummer2026.dto.staff.StaffCapabilityRequest;
import org.example.doansummer2026.dto.staff.StaffCapabilityResponse;
import org.example.doansummer2026.dto.staff.ClinicManagerStaffResponse;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.service.StaffScheduleService;
import org.example.doansummer2026.service.StaffService;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final StaffScheduleService staffScheduleService;
    private final ShiftConfigRepository shiftConfigRepo;

    @GetMapping("/clinic-manager")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<ClinicManagerStaffResponse>> searchForClinicManager(
            @RequestParam(required = false) String search, Pageable pageable) {
        return RestResponses.ok(staffService.searchForClinicManager(search, pageable));
    }

    @GetMapping("/clinic-manager/{id}")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ClinicManagerStaffResponse> getForClinicManager(@PathVariable UUID id) {
        return RestResponses.ok(staffService.getForClinicManager(id));
    }

    @GetMapping("/{staffId}/capabilities")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_STAFF')")
    public List<StaffCapabilityResponse> listCapabilities(@PathVariable UUID staffId) {
        return staffService.listCapabilities(staffId);
    }

    @PutMapping("/{staffId}/capabilities")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<StaffCapabilityResponse> replaceCapabilities(@PathVariable UUID staffId,
            @RequestBody List<StaffCapabilityRequest> requests) {
        return staffService.replaceCapabilities(staffId, requests);
    }

    /** ADMIN vaf CLINIC_MANAGER xem danh sach. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<PageResponse<StaffResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID specializationId,
            @RequestParam(required = false) SystemRole systemRole,
            Pageable pageable) {
        return RestResponses.ok(staffService.search(search, specializationId, systemRole, pageable));
    }

    /** ADMIN vaf CLINIC_MANAGER xem danh sach nhan su don gian (cho Schedule). */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
    public ResponseEntity<List<StaffOptionResponse>> list(
            @RequestParam(required = false) SystemRole systemRole) {
        return RestResponses.ok(staffService.listForSchedule(systemRole));
    }

    /** Nhan su xem lich ca nhan. */
    @GetMapping("/my-schedule")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<MyScheduleResponse> mySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        LocalDate weekStart = week.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Lay staffId tu SecurityContext
        UUID staffId = getCurrentStaffId();
        if (staffId == null) {
            return RestResponses.ok(new MyScheduleResponse(List.of(), Map.of(), null));
        }

        var schedules = staffScheduleService.findByStaffAndWeek(staffId, weekStart, weekEnd);
        var response = MyScheduleResponse.from(schedules, staffId, shiftConfigRepo.findAll());
        return RestResponses.ok(response);
    }

    private UUID getCurrentStaffId() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal() instanceof Map<?, ?> map
                ? UUID.fromString((String) map.get("staffId"))
                : null;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<StaffResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(staffService.get(id));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_RECEPTIONIST', 'ROLE_CASHIER')")
    public ResponseEntity<StaffResponse> getByAccount(@PathVariable UUID accountId) {
        return RestResponses.ok(staffService.getByAccountId(accountId));
    }

    /** Nhan vien tu cap nhat hoc vi va truong dao tao; chuyen khoa khong nam trong request. */
    @PutMapping("/me/professional")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_RECEPTIONIST', 'ROLE_CASHIER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "StaffInfo")
    public ResponseEntity<StaffResponse> updateOwnProfessionalInfo(
            @Valid @RequestBody StaffProfessionalUpdateRequest req) {
        UUID staffId = getCurrentStaffId();
        if (staffId == null) {
            throw new org.example.doansummer2026.exception.BadRequestException("Khong xac dinh duoc nhan vien hien tai");
        }
        return RestResponses.ok(staffService.updateOwnProfessionalInfo(staffId, req));
    }

    /** CHI ADMIN tao nhan vien moi (kem account + profile). */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "StaffInfo")
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffCreateRequest req) {
        StaffResponse created = staffService.create(req);
        return RestResponses.created("/api/v1/staff/{id}", created.staffId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "StaffInfo")
    public ResponseEntity<StaffResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody StaffUpdateRequest req) {
        return RestResponses.ok(staffService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "StaffInfo", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffService.delete(id);
        return RestResponses.noContent();
    }

    /** ADMIN vaf CLINIC_MANAGER khoa tai khoan (KHONG cho phep khoa ADMIN/CLINIC_MANAGER). */
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "StaffInfo", idParamName = "id")
    public ResponseEntity<StaffResponse> lock(@PathVariable UUID id) {
        StaffResponse locked = staffService.lock(id);
        return RestResponses.ok(locked);
    }
}
