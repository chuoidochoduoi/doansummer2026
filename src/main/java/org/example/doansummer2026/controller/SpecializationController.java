package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.specialization.SpecializationCreateRequest;
import org.example.doansummer2026.dto.specialization.SpecializationResponse;
import org.example.doansummer2026.dto.specialization.SpecializationUpdateRequest;
import org.example.doansummer2026.service.SpecializationService;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<PageResponse<SpecializationResponse>> list(Pageable pageable) {
        return RestResponses.ok(service.list(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<SpecializationResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "Specialization")
    public ResponseEntity<SpecializationResponse> create(@Valid @RequestBody SpecializationCreateRequest req) {
        SpecializationResponse created = service.create(req);
        return RestResponses.created("/api/v1/specializations/{id}", created.specializationId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Specialization", idParamName = "id")
    public ResponseEntity<SpecializationResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody SpecializationUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "Specialization", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}



