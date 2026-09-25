package vn.edu.fpt.cares.dto.auditlog;

import vn.edu.fpt.cares.enums.AuditAction;
import vn.edu.fpt.cares.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID auditId,
        AuditAction action,
        String entityName,
        String entityId,
        UUID actorAccountId,
        String actorName,
        String ipAddress,
        String userAgent,
        String oldValueJson,
        String newValueJson,
        String description,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getAuditId(), a.getAction(), a.getEntityName(),
                a.getEntityId(), a.getActorAccountId(), null, a.getIpAddress(), a.getUserAgent(),
                a.getOldValueJson(), a.getNewValueJson(), a.getDescription(), a.getCreatedAt());
    }

    public static AuditLogResponse from(AuditLog a, String actorName) {
        return new AuditLogResponse(a.getAuditId(), a.getAction(), a.getEntityName(),
                a.getEntityId(), a.getActorAccountId(), actorName, a.getIpAddress(), a.getUserAgent(),
                a.getOldValueJson(), a.getNewValueJson(), a.getDescription(), a.getCreatedAt());
    }
}




