package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.auditlog.AuditLogCreateRequest;
import vn.edu.fpt.cares.dto.auditlog.AuditLogResponse;
import vn.edu.fpt.cares.enums.AuditAction;
import vn.edu.fpt.cares.service.AuditLogService;
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<List<AuditLogResponse>> byEntity(
            @RequestParam String entityName,
            @RequestParam String entityId) {
        return RestResponses.ok(service.findByEntity(entityName, entityId));
    }

}



