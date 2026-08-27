package org.example.doansummer2026.model;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.ClinicalTemplateStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinical_form_template_version", uniqueConstraints = @UniqueConstraint(
        name = "uk_clinical_template_version", columnNames = {"template_id", "version_no"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClinicalFormTemplateVersion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id")
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ClinicalFormTemplate template;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode schemaJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicalTemplateStatus status;

    @Column(name = "change_reason", nullable = false, length = 1000)
    private String changeReason;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private StaffInfo createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private StaffInfo publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
