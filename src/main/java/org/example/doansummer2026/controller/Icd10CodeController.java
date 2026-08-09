package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.icd.ICD10CreateRequest;
import org.example.doansummer2026.dto.icd.ICD10Response;
import org.example.doansummer2026.dto.icd.ICD10UpdateRequest;
import org.example.doansummer2026.service.Icd10CodeService;
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

/**
 * API quan ly ICD-10 codes - danh muc benh cho chuan doan.
 * - GET /api/v1/icd10-codes: Tim kiem (keyword, category)
 */
@RestController
@RequestMapping("/api/v1/icd10-codes")
@RequiredArgsConstructor
public class Icd10CodeController {

    private final Icd10CodeService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN','ROLE_STAFF')")
    public ResponseEntity<PageResponse<ICD10Response>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return RestResponses.ok(service.search(keyword, category, pageable));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN','ROLE_STAFF')")
    public ResponseEntity<ICD10Response> get(@PathVariable String code) {
        return RestResponses.ok(service.get(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ICD10Response> create(@Valid @RequestBody ICD10CreateRequest req) {
        ICD10Response created = service.create(req);
        return RestResponses.created("/api/v1/icd10-codes/{code}", created.code(), created);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ICD10Response> update(@PathVariable String code,
                                               @Valid @RequestBody ICD10UpdateRequest req) {
        return RestResponses.ok(service.update(code, req));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return RestResponses.noContent();
    }
}
