package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.clinic.ClinicInformationRequest;
import org.example.doansummer2026.dto.clinic.ClinicInformationResponse;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.service.ClinicInformationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClinicInformationController {
    private final ClinicInformationService service;

    @GetMapping("/api/public/clinic-information")
    public ResponseEntity<ClinicInformationResponse> publicInformation() {
        return RestResponses.ok(service.get());
    }

    @GetMapping("/api/v1/clinic-information")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    public ResponseEntity<ClinicInformationResponse> information() {
        return RestResponses.ok(service.get());
    }

    @PutMapping("/api/v1/clinic-information")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "ClinicInformation",
            description = "Cập nhật thông tin phòng khám")
    public ResponseEntity<ClinicInformationResponse> update(
            @Valid @RequestBody ClinicInformationRequest request) {
        return RestResponses.ok(service.update(request));
    }
}
