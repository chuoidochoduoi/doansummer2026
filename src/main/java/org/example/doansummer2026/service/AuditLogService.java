package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest;
import org.example.doansummer2026.dto.auditLog.AuditLogResponse;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.model.AuditLog;
import org.example.doansummer2026.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.AuditLogServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditLogService implements AuditLogServiceInterface {

    private final AuditLogRepository repo;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(UUID actorId, AuditAction action, String entityName,
                                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<AuditLog> page = repo.search(actorId, action, entityName, from, to, pageable);
        return PageResponse.from(page, AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByEntity(String entityName, String entityId) {
        return repo.findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId)
                .stream().map(AuditLogResponse::from).toList();
    }

    public AuditLogResponse create(AuditLogCreateRequest req) {
        AuditLog a = AuditLog.builder()
                .action(req.action())
                .entityName(req.entityName())
                .entityId(req.entityId())
                .actorAccountId(req.actorAccountId())
                .ipAddress(req.ipAddress())
                .userAgent(req.userAgent())
                .oldValueJson(req.oldValueJson())
                .newValueJson(req.newValueJson())
                .description(req.description())
                .createdAt(LocalDateTime.now())
                .build();
        return AuditLogResponse.from(repo.save(a));
    }
}
