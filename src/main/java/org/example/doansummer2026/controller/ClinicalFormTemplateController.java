package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.clinicalForm.*;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.service.ClinicalFormTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinical-form-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CLINIC_MANAGER')")
public class ClinicalFormTemplateController {
    private final ClinicalFormTemplateService service;

    @GetMapping
    public ResponseEntity<List<ClinicalFormTemplateResponse>> list() { return RestResponses.ok(service.list()); }

    @PostMapping
    @Auditable(action = AuditAction.CREATE, entityName = "ClinicalFormTemplate")
    public ResponseEntity<ClinicalFormTemplateResponse> create(@Valid @RequestBody ClinicalFormTemplateRequest req) {
        ClinicalFormTemplateResponse created = service.create(req);
        return RestResponses.created("/api/v1/clinical-form-templates/{id}", created.templateId(), created);
    }

    @PutMapping("/{id}/draft")
    @Auditable(action = AuditAction.UPDATE, entityName = "ClinicalFormTemplate", idParamName = "id")
    public ResponseEntity<ClinicalFormTemplateResponse> draft(@PathVariable UUID id, @Valid @RequestBody ClinicalFormDraftRequest req) {
        return RestResponses.ok(service.saveDraft(id, req));
    }

    @PostMapping("/{id}/publish")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ClinicalFormTemplate", idParamName = "id")
    public ResponseEntity<ClinicalFormTemplateResponse> publish(@PathVariable UUID id) { return RestResponses.ok(service.publish(id)); }

    @PostMapping("/{id}/retire")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ClinicalFormTemplate", idParamName = "id")
    public ResponseEntity<ClinicalFormTemplateResponse> retire(@PathVariable UUID id) { return RestResponses.ok(service.retire(id)); }

    @PutMapping("/{id}/services")
    @Auditable(action = AuditAction.UPDATE, entityName = "ClinicalFormTemplateBinding", idParamName = "id")
    public ResponseEntity<ClinicalFormTemplateResponse> bind(@PathVariable UUID id, @Valid @RequestBody ClinicalFormBindingRequest req) {
        return RestResponses.ok(service.bindServices(id, req));
    }
}
