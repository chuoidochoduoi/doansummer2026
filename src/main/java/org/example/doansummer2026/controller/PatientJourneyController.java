package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.service.PatientJourneyService;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

@RestController @RequiredArgsConstructor
public class PatientJourneyController {
    private final PatientJourneyService service;
    private final org.example.doansummer2026.service.AuthService authService;
    @GetMapping("/api/v1/patient-journeys")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN','ROLE_DOCTOR','ROLE_NURSE')")
    public ResponseEntity<PageResponse<PatientJourneyResponse>> list(@RequestParam(required=false) String search,
                                                                       @RequestParam(required=false) String status,
                                                                       Pageable pageable) {
        return RestResponses.ok(service.list(search, status, pageable));
    }
    @GetMapping("/api/v1/patient-journeys/{visitId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN','ROLE_DOCTOR','ROLE_NURSE')")
    public ResponseEntity<PatientJourneyResponse> get(@PathVariable UUID visitId) { return RestResponses.ok(service.get(visitId)); }

    @PostMapping("/api/v1/patient-journeys/{visitId}/advance")
    @PreAuthorize("hasAnyAuthority('ROLE_CLINIC_MANAGER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "CustomerVisit", idParamName = "visitId", description = "Phuc hoi buoc hang cho bi ket")
    public ResponseEntity<PatientJourneyResponse> advance(@PathVariable UUID visitId) {
        return RestResponses.ok(service.advanceBlockedStep(visitId));
    }
    @GetMapping("/api/patient/my-journeys")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<List<PatientJourneyResponse>> mine() {
        return RestResponses.ok(service.listForCustomer(authService.currentProfileId()));
    }

    @GetMapping("/api/public/patient-journeys/lookup")
    public ResponseEntity<PatientJourneyResponse> lookupGuest(@RequestParam String visitCode,
                                                               @RequestParam String phone) {
        return RestResponses.ok(service.lookupGuest(visitCode, phone));
    }
}
