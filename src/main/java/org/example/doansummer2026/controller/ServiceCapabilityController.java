package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.capability.*;
import org.example.doansummer2026.service.ServiceCapabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/service-capabilities") @RequiredArgsConstructor
public class ServiceCapabilityController {
    private final ServiceCapabilityService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER','ROLE_STAFF')")
    public List<ServiceCapabilityResponse> list() { return service.list(); }

    @PostMapping @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ServiceCapabilityResponse create(@Valid @RequestBody ServiceCapabilityRequest request) { return service.create(request); }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ServiceCapabilityResponse update(@PathVariable UUID id, @Valid @RequestBody ServiceCapabilityRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
