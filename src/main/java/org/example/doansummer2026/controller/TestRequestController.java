package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.testRequest.TestRequestBatchCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestUpdateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestCancelRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-requests")
@RequiredArgsConstructor
public class TestRequestController {

    private final TestRequestService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<PageResponse<TestRequestResponse>> list(
            @RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) TestRequestStatus status,
            Pageable pageable) {
        return RestResponses.ok(service.search(recordId, departmentId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> create(@Valid @RequestBody TestRequestCreateRequest req) {
        TestRequestResponse created = service.create(req);
        return RestResponses.created("/api/v1/test-requests/{id}", created.testRequestId(), created);
    }

    /**
     * Tao nhieu TestRequest cung luc - bac si chon nhieu dich vu xet nghiem.
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<List<TestRequestResponse>> createBatch(
            @Valid @RequestBody TestRequestBatchCreateRequest req) {
        List<TestRequestResponse> created = service.createBatch(req);
        return RestResponses.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody TestRequestUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * Huy yeu cau xet nghiem - chi cho PENDING hoac IN_PROGRESS.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestRequestResponse> cancel(@PathVariable UUID id,
                                                      @Valid @RequestBody TestRequestCancelRequest req) {
        return RestResponses.ok(service.cancel(id, req));
    }

    // --- TestResult sub-resource ---

    @GetMapping("/{id}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> getResult(@PathVariable UUID id) {
        return RestResponses.ok(service.getResult(id));
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> createResult(@PathVariable UUID id,
                                                            @Valid @RequestBody TestResultCreateRequest req) {
        TestResultResponse created = service.createResult(id, req);
        return RestResponses.created("/api/v1/test-requests/{id}/result", created.resultId(), created);
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> updateResult(@PathVariable UUID id,
                                                            @Valid @RequestBody TestResultUpdateRequest req) {
        return RestResponses.ok(service.updateResult(id, req));
    }

    /**
     * Hoan thanh ket qua xet nghiem - CAP NHAT VA CHUYEN STATUS SANG COMPLETED.
     * Neu chua co ket qua thi tao moi, neu co roi thi cap nhat.
     */
    @PostMapping("/{id}/result/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<TestResultResponse> completeResult(@PathVariable UUID id,
                                                                @Valid @RequestBody TestResultCreateRequest req) {
        return RestResponses.ok(service.completeResult(id, req));
    }

    // --- Upload file ket qua ---

    /**
     * Upload file ket qua xet nghiem.
     * Luu file vao thu muc local va tra ve URL va ten file.
     */
    @PostMapping("/{id}/upload")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ADMIN')")
    public ResponseEntity<Map<String, String>> uploadResult(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = service.uploadResultFile(id, file);
        String fileName = file.getOriginalFilename();
        return RestResponses.ok(Map.of("imageUrl", imageUrl, "fileName", fileName));
    }
}





