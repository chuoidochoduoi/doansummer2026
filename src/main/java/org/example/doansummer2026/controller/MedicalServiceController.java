package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalService.MedicalServiceCreateRequest;
import org.example.doansummer2026.dto.medicalService.MedicalServiceResponse;
import org.example.doansummer2026.dto.medicalService.MedicalServiceUpdateRequest;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.service.MedicalServiceService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-services")
@RequiredArgsConstructor
public class MedicalServiceController {

    private final MedicalServiceService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<PageResponse<MedicalServiceResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DepartmentType departmentType,
            @RequestParam(required = false) ServiceStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(keyword, departmentType, status, pageable));
    }

    /**
     * API cho khach hang/benh nhan xem dich vu dang hoat dong.
     * Chi tra ve cac dich vu co status = ACTIVE.
     */
    @GetMapping("/available")
    public ResponseEntity<PageResponse<MedicalServiceResponse>> listAvailable(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DepartmentType departmentType,
            Pageable pageable) {
        return RestResponses.ok(service.listAvailable(keyword, departmentType, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<MedicalServiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF', 'ROLE_DOCTOR')")
    public ResponseEntity<Map<String, Long>> getStats() {
        return RestResponses.ok(service.getStats());
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

    /** Ngung hoat dong dich vu - chi dich vu ACTIVE moi duoc ngung. */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalServiceResponse> deactivate(@PathVariable UUID id) {
        return RestResponses.ok(service.deactivate(id));
    }

    /** Phat hanh dich vu - chi dich vu DRAFT moi duoc phat hanh. */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalServiceResponse> publish(@PathVariable UUID id) {
        return RestResponses.ok(service.publish(id));
    }
}




