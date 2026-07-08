package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.enums.ServiceType;
import org.example.doansummer2026.service.MedicalServiceService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-services")
@RequiredArgsConstructor
public class MedicalServiceController {

    private final MedicalServiceService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<PageResponse<MedicalServiceResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        return RestResponses.ok(service.search(keyword, categoryId, serviceType, isActive, pageable));
    }

    /**
     * API cho khach hang/benh nhan xem dich vu dang hoat dong.
     * Chi tra ve cac dich vu co isActive = true.
     */
    @GetMapping("/available")
    public ResponseEntity<PageResponse<MedicalServiceResponse>> listAvailable(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ServiceType serviceType,
            Pageable pageable) {
        return RestResponses.ok(service.listAvailable(keyword, categoryId, serviceType, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<MedicalServiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalServiceResponse> create(@Valid @RequestBody MedicalServiceCreateRequest req) {
        MedicalServiceResponse created = service.create(req);
        return RestResponses.created("/api/v1/medical-services/{id}", created.serviceId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalServiceResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalServiceUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}
