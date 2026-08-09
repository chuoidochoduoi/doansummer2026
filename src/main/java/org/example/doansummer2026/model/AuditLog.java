package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;
import org.example.doansummer2026.enums.AuditAction;

/**
 * Audit log - ghi lai moi thao tac CRUD cua user.
 * - action: CREATE/UPDATE/DELETE/LOGIN/LOGOUT/EXPORT...
 * - entityName + entityId: entity bi tac dong.
 * - actorAccountId: account thuc hien (UUID - nullable cho system action).
 * - oldValue/newValue: JSON snapshot (optional, chi khi can).
 * - ipAddress, userAgent: thong tin request.
 *
 * Khong extend BaseEntity (can createdAt - audit chinh la createdAt).
 */
@Entity
@Table(name = "audit_log",
        indexes = {
                @jakarta.persistence.Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
                @jakarta.persistence.Index(name = "idx_audit_actor", columnList = "actor_account_id"),
                @jakarta.persistence.Index(name = "idx_audit_created_at", columnList = "created_at")
        })
@SQLDelete(sql = "UPDATE audit_log SET deleted = true WHERE audit_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    private UUID auditId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(name = "actor_account_id")
    private UUID actorAccountId;

    @Size(max = 50)
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValueJson;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValueJson;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}




