package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.RestResponses;
import org.springframework.http.ResponseEntity;
import vn.edu.fpt.cares.dto.shift.ShiftConfigResponse;
import vn.edu.fpt.cares.service.ShiftConfigService;
import vn.edu.fpt.cares.service.ShiftAvailabilityService;
import vn.edu.fpt.cares.service.ClinicScheduleManagementService;
import vn.edu.fpt.cares.dto.shift.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Set;
import vn.edu.fpt.cares.aop.Auditable;
import vn.edu.fpt.cares.enums.AuditAction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftConfigController {

    private final ShiftConfigService shiftConfigService;
    private final ShiftAvailabilityService shiftAvailabilityService;
    private final ClinicScheduleManagementService scheduleManagementService;

    // Public API cho bệnh nhân/lễ tân xem lịch ca khám
    @GetMapping("/active")
    public ResponseEntity<List<ShiftConfigResponse>> getActiveShifts() {
        return RestResponses.ok(shiftConfigService.getAllActiveShifts());
    }

    @GetMapping("/available")
    public ResponseEntity<List<AvailableShiftResponse>> getAvailableShifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Set<UUID> serviceIds) {
        return RestResponses.ok(shiftAvailabilityService.available(date, serviceIds));
    }

    // Admin APIs
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<List<ShiftConfigResponse>> getAllShifts() {
        return RestResponses.ok(shiftConfigService.getAllShifts());
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<List<ShiftVersionResponse>> versions(@PathVariable UUID id) {
        return RestResponses.ok(scheduleManagementService.history(id));
    }

    @GetMapping("/{id}/versions/impact")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ScheduleImpactResponse> versionImpact(@PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom) {
        return RestResponses.ok(scheduleManagementService.previewVersionImpact(id, effectiveFrom));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.CREATE, entityName = "ShiftVersion", idParamName = "id")
    public ResponseEntity<ShiftVersionResponse> createVersion(@PathVariable UUID id,
            @Valid @RequestBody ShiftVersionCreateRequest request) {
        return RestResponses.ok(scheduleManagementService.createVersion(id, request));
    }
}
