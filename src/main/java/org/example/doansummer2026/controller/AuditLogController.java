package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest;
import org.example.doansummer2026.dto.auditLog.AuditLogResponse;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.service.AuditLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(actorId, action, entityName, from, to, pageable));
    }

    @GetMapping("/by-entity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> byEntity(
            @RequestParam String entityName,
            @RequestParam String entityId) {
        return RestResponses.ok(service.findByEntity(entityName, entityId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogResponse> create(@Valid @RequestBody AuditLogCreateRequest req) {
        AuditLogResponse created = service.create(req);
        return RestResponses.created("/api/v1/audit-logs/{id}", created.auditId(), created);
    }
}
