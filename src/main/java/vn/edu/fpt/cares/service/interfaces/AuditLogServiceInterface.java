package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.auditlog.AuditLogResponse;
import vn.edu.fpt.cares.dto.auditlog.AuditLogCreateRequest;
import vn.edu.fpt.cares.enums.AuditAction;
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



