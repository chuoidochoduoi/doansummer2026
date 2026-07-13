package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsCreateRequest;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsResponse;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsUpdateRequest;
import org.example.doansummer2026.service.VitalSignsService;
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
@RequestMapping("/api/v1/vital-signs")
@RequiredArgsConstructor
public class VitalSignsController {

    private final VitalSignsService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<VitalSignsResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<VitalSignsResponse> create(@Valid @RequestBody VitalSignsCreateRequest req) {
        VitalSignsResponse created = service.create(req);
        return RestResponses.created("/api/v1/vital-signs/{id}", created.vitalId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<VitalSignsResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody VitalSignsUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}
