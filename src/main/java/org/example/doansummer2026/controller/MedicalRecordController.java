package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.service.MedicalRecordService;
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

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<PageResponse<MedicalRecordResponse>> list(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) MedicalRecordStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(doctorId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordCreateRequest req) {
        MedicalRecordResponse created = service.create(req);
        return RestResponses.created("/api/v1/medical-records/{id}", created.recordId(), created);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }
    /**
     * Lục nháp - lưu tạm các thay đổi (chưa thay đổi status).
     */
    @PostMapping("/{id}/draft")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> saveDraft(@PathVariable UUID id,
                                                           @Valid @RequestBody MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.saveDraft(id, req));
    }
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> complete(@PathVariable UUID id,
                                                          @Valid @RequestBody(required = false) MedicalRecordUpdateRequest req) {
        return RestResponses.ok(service.complete(id, req));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * API danh gia phong kham (1-5 sao). Chi ap dung cho EXAMINATION da hoan thanh (COMPLETED).
     */
    @PostMapping("/{id}/rate")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ADMIN')")
    public ResponseEntity<MedicalRecordResponse> rate(@PathVariable UUID id,
                                                     @RequestParam int ratingScore) {
        return RestResponses.ok(service.rate(id, ratingScore));
    }
}




