package org.example.doansummer2026.dto.auditLog;

import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID auditId,
        AuditAction action,
        String entityName,
        String entityId,
        UUID actorAccountId,
        String ipAddress,
        String userAgent,
        String oldValueJson,
        String newValueJson,
        String description,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getAuditId(), a.getAction(), a.getEntityName(),
                a.getEntityId(), a.getActorAccountId(), a.getIpAddress(), a.getUserAgent(),
                a.getOldValueJson(), a.getNewValueJson(), a.getDescription(), a.getCreatedAt());
    }
}
