package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.service.PatientJourneyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequiredArgsConstructor
public class PatientJourneyController {
    private final PatientJourneyService service;
    private final org.example.doansummer2026.service.AuthService authService;
    @GetMapping("/api/v1/patient-journeys")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN','ROLE_DOCTOR','ROLE_NURSE')")
    public ResponseEntity<List<PatientJourneyResponse>> list(@RequestParam(required=false) String search, @RequestParam(required=false) String status) {
        return RestResponses.ok(service.list(search,status));
    }
    @GetMapping("/api/v1/patient-journeys/{visitId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER','ROLE_ADMIN','ROLE_DOCTOR','ROLE_NURSE')")
    public ResponseEntity<PatientJourneyResponse> get(@PathVariable UUID visitId) { return RestResponses.ok(service.get(visitId)); }
    @GetMapping("/api/patient/my-journeys")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<List<PatientJourneyResponse>> mine() {
        return RestResponses.ok(service.listForCustomer(authService.currentProfileId()));
    }
}
