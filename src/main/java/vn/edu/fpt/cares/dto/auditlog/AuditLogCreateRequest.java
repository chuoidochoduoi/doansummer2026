package vn.edu.fpt.cares.dto.auditlog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.cares.enums.AuditAction;

import java.util.UUID;

public record AuditLogCreateRequest(
        @NotNull AuditAction action,
        @NotBlank @Size(max = 100) String entityName,
        @Size(max = 50) String entityId,
        UUID actorAccountId,
        @Size(max = 50) String ipAddress,
        @Size(max = 500) String userAgent,
        String oldValueJson,
        String newValueJson,
        String description
) {}




