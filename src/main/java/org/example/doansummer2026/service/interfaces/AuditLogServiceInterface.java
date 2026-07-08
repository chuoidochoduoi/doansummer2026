package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.auditLog.AuditLogResponse;
import org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest;
import org.example.doansummer2026.enums.AuditAction;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Service interface for AuditLog management. */
public interface AuditLogServiceInterface {
    PageResponse<AuditLogResponse> search(UUID actorId, AuditAction action, String entityName,
                                           LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<AuditLogResponse> findByEntity(String entityName, String entityId);
    AuditLogResponse create(AuditLogCreateRequest req);
}