package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.shift.*;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/clinic-schedule")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
public class ClinicScheduleController {
    private final ClinicScheduleManagementService managementService;
    private final ServiceAvailabilityService availabilityService;

    @GetMapping("/exceptions")
    public ResponseEntity<List<ClinicScheduleExceptionResponse>> exceptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return RestResponses.ok(managementService.exceptions(from, to));
    }

    @PostMapping("/exceptions/impact")
    public ResponseEntity<ScheduleImpactResponse> previewException(
            @Valid @RequestBody ClinicScheduleExceptionRequest request) {
        return RestResponses.ok(managementService.previewExceptionImpact(request));
    }

    @PostMapping("/exceptions")
    @Auditable(action = AuditAction.CREATE, entityName = "ClinicScheduleException")
    public ResponseEntity<ClinicScheduleExceptionResponse> createException(
            @Valid @RequestBody ClinicScheduleExceptionRequest request) {
        return RestResponses.ok(managementService.createException(request));
    }

    @DeleteMapping("/exceptions/{id}")
    @Auditable(action = AuditAction.DELETE, entityName = "ClinicScheduleException", idParamName = "id")
    public ResponseEntity<Void> reopen(@PathVariable UUID id) {
        managementService.reopen(id);
        return RestResponses.noContent();
    }

    @GetMapping("/coverage")
    public ResponseEntity<ServiceCoverageResponse> coverage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID shiftId) {
        return RestResponses.ok(availabilityService.coverage(date, shiftId));
    }
}
