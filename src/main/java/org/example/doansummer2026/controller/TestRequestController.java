package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.testRequest.TestRequestBatchCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestUpdateRequest;
import org.example.doansummer2026.dto.testResult.TestResultCreateRequest;
import org.example.doansummer2026.dto.testResult.TestResultResponse;
import org.example.doansummer2026.dto.testResult.TestResultUpdateRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.service.TestRequestService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-requests")
@RequiredArgsConstructor
public class TestRequestController {

    private final TestRequestService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<PageResponse<TestRequestResponse>> list(
            @RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) TestRequestStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(recordId, departmentId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> create(@Valid @RequestBody TestRequestCreateRequest req) {
        TestRequestResponse created = service.create(req);
        return RestResponses.created("/api/v1/test-requests/{id}", created.testRequestId(), created);
    }

    /**
     * Tao nhieu TestRequest cung luc - bac si chon nhieu dich vu xet nghiem.
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<List<TestRequestResponse>> createBatch(
            @Valid @RequestBody TestRequestBatchCreateRequest req) {
        List<TestRequestResponse> created = service.createBatch(req);
        return RestResponses.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody TestRequestUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    // --- TestResult sub-resource ---

    @GetMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> getResult(@PathVariable UUID id) {
        return RestResponses.ok(service.getResult(id));
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> createResult(@PathVariable UUID id,
                                                            @Valid @RequestBody TestResultCreateRequest req) {
        TestResultResponse created = service.createResult(id, req);
        return RestResponses.created("/api/v1/test-requests/{id}/result", created.resultId(), created);
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> updateResult(@PathVariable UUID id,
                                                            @Valid @RequestBody TestResultUpdateRequest req) {
        return RestResponses.ok(service.updateResult(id, req));
    }
}
