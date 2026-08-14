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
import org.example.doansummer2026.service.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

@RestController
@RequestMapping("/api/v1/test-requests")
@RequiredArgsConstructor
public class TestRequestController {

    private final TestRequestService service;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<TestRequestResponse>> list(
            @RequestParam(required = false) UUID recordId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) TestRequestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate workDate,
            Pageable pageable) {
        return RestResponses.ok(service.search(recordId, departmentId, status, search, workDate, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<TestRequestResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    /** Danh sach yeu cau CLS trong toan bo luot kham, dung de chan chi dinh trung. */
    @GetMapping("/visit/{visitId}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<List<TestRequestResponse>> listByVisit(@PathVariable UUID visitId) {
        return RestResponses.ok(service.listByVisit(visitId));
    }

    @GetMapping("/queue/{ticketId}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<List<TestRequestResponse>> listByQueue(@PathVariable UUID ticketId) {
        return RestResponses.ok(service.listByQueueTicket(ticketId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "TestRequest")
    public ResponseEntity<TestRequestResponse> create(@Valid @RequestBody TestRequestCreateRequest req) {
        TestRequestResponse created = service.create(req);
        return RestResponses.created("/api/v1/test-requests/{id}", created.testRequestId(), created);
    }

    /**
     * Tao nhieu TestRequest cung luc - bac si chon nhieu dich vu xet nghiem.
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "TestRequest", description = "Tạo danh sách yêu cầu cận lâm sàng")
    public ResponseEntity<List<TestRequestResponse>> createBatch(
            @Valid @RequestBody TestRequestBatchCreateRequest req) {
        List<TestRequestResponse> created = service.createBatch(req);
        return RestResponses.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "TestRequest", idParamName = "id")
    public ResponseEntity<TestRequestResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody TestRequestUpdateRequest req,
                                                       org.springframework.security.core.Authentication auth) {
        if (req.status() == org.example.doansummer2026.enums.TestRequestStatus.COMPLETED) {
            boolean isDoctorOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR") || a.getAuthority().equals("ADMIN") 
                            || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("GENERAL_DOCTOR") || a.getAuthority().equals("SPECIALIST_DOCTOR"));
            if (!isDoctorOrAdmin) {
                throw new org.springframework.security.access.AccessDeniedException("Chỉ bác sĩ mới có quyền hoàn thành kết quả cận lâm sàng");
            }
        }
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "TestRequestCancel", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * Huy yeu cau xet nghiem - chi cho PENDING hoac IN_PROGRESS.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "TestRequest", idParamName = "id", description = "Hủy yêu cầu cận lâm sàng")
    public ResponseEntity<TestRequestResponse> cancel(@PathVariable UUID id,
                                                      @Valid @RequestBody TestRequestCancelRequest req) {
        return RestResponses.ok(service.cancel(id, req));
    }

    /**
     * Tim TestRequest theo InvoiceItem (traceability: Invoice -> InvoiceItem -> TestRequest).
     */
    @GetMapping("/by-invoice-item/{itemId}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<List<TestRequestResponse>> findByInvoiceItem(@PathVariable UUID itemId) {
        return RestResponses.ok(service.findByInvoiceItem(itemId));
    }

    /**
     * Tim TestRequest theo Invoice (traceability: Invoice -> InvoiceItem -> TestRequest).
     */
    @GetMapping("/by-invoice/{invoiceId}")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<List<TestRequestResponse>> findByInvoice(@PathVariable UUID invoiceId) {
        return RestResponses.ok(service.findByInvoice(invoiceId));
    }

    // --- TestResult sub-resource ---

    @GetMapping("/{id}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    public ResponseEntity<TestResultResponse> getResult(@PathVariable UUID id) {
        return RestResponses.ok(service.getResult(id));
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "TestResult", idParamName = "id", description = "Tạo nháp kết quả cận lâm sàng")
    public ResponseEntity<TestResultResponse> createResult(@PathVariable UUID id,
                                                            @Valid @RequestBody TestResultCreateRequest req) {
        TestResultResponse created = service.createResult(id, req);
        return RestResponses.created("/api/v1/test-requests/{id}/result", created.resultId(), created);
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "TestResult", idParamName = "id", description = "Cập nhật kết quả cận lâm sàng")
    public ResponseEntity<TestResultResponse> updateResult(@PathVariable UUID id,
                                                            @Valid @RequestBody TestResultUpdateRequest req) {
        return RestResponses.ok(service.updateResult(id, req));
    }

    /**
     * Hoan thanh ket qua xet nghiem - CAP NHAT VA CHUYEN STATUS SANG COMPLETED.
     * Neu chua co ket qua thi tao moi, neu co roi thi cap nhat.
     */
    @PostMapping("/{id}/result/complete")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    @Auditable(action = AuditAction.RESULT_SIGNED, entityName = "TestRequest", idParamName = "id", description = "Bác sĩ ký xác nhận kết quả cận lâm sàng")
    public ResponseEntity<TestResultResponse> completeResult(@PathVariable UUID id,
                                                                @Valid @RequestBody TestResultCreateRequest req) {
        return RestResponses.ok(service.completeResult(id, req, authService.currentStaffId()));
    }

    // --- Upload file ket qua ---

    /**
     * Upload file ket qua xet nghiem.
     * Luu file vao thu muc local va tra ve URL va ten file.
     */
    @PostMapping("/{id}/upload")
    @PreAuthorize("hasAnyAuthority('ROLE_NURSE','ROLE_DOCTOR','ROLE_ADMIN')")
    @Auditable(action = AuditAction.RESULT_UPLOADED, entityName = "TestRequest", idParamName = "id", description = "Tải phiếu kết quả PDF")
    public ResponseEntity<Map<String, String>> uploadResult(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = service.uploadResultFile(id, file);
        String fileName = file.getOriginalFilename();
        return RestResponses.ok(Map.of("imageUrl", imageUrl, "fileName", fileName));
    }
}

