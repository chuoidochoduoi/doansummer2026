package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.contact.ContactRequestCreateRequest;
import org.example.doansummer2026.dto.contact.ContactRequestResolveRequest;
import org.example.doansummer2026.dto.contact.ContactRequestResponse;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.enums.ContactRequestStatus;
import org.example.doansummer2026.service.ContactRequestService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ContactRequestController {

    private final ContactRequestService service;

    @PostMapping("/api/public/contact-requests")
    public ResponseEntity<ContactRequestResponse> create(
            @Valid @RequestBody ContactRequestCreateRequest request) {
        ContactRequestResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/v1/contact-requests")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<ContactRequestResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ContactRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return RestResponses.ok(service.search(search, status, fromDate, toDate, pageable));
    }

    @GetMapping("/api/v1/contact-requests/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ContactRequestResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @GetMapping("/api/v1/contact-requests/stats/new-count")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<Map<String, Long>> countNew() {
        return RestResponses.ok(Map.of("count", service.countNew()));
    }

    @PostMapping("/api/v1/contact-requests/{id}/accept")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ContactRequest", idParamName = "id",
            description = "Tiếp nhận yêu cầu liên hệ")
    public ResponseEntity<ContactRequestResponse> accept(@PathVariable UUID id) {
        return RestResponses.ok(service.accept(id));
    }

    @PostMapping("/api/v1/contact-requests/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ContactRequest", idParamName = "id",
            description = "Xác nhận đã liên hệ khách hàng")
    public ResponseEntity<ContactRequestResponse> complete(
            @PathVariable UUID id, @Valid @RequestBody ContactRequestResolveRequest request) {
        return RestResponses.ok(service.complete(id, request));
    }

    @PostMapping("/api/v1/contact-requests/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "ContactRequest", idParamName = "id",
            description = "Ghi nhận không thể liên hệ khách hàng")
    public ResponseEntity<ContactRequestResponse> cancel(
            @PathVariable UUID id, @Valid @RequestBody ContactRequestResolveRequest request) {
        return RestResponses.ok(service.cancel(id, request));
    }
}
