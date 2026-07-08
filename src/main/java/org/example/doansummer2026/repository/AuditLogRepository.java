package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, String entityId);

    List<AuditLog> findByActorAccountIdOrderByCreatedAtDesc(UUID actorAccountId);

    default Page<AuditLog> search(UUID actorId, AuditAction action,
                                  String entityName, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorAccountId"), actorId));
        }
        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (entityName != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityName"), entityName));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return findAll(spec, pageable);
    }
}