package org.example.doansummer2026.model;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.TestResultRevisionStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_result_revision", uniqueConstraints = @UniqueConstraint(
        name = "uk_test_result_revision", columnNames = {"result_id", "revision_no"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TestResultRevision extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "revision_id")
    private UUID revisionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private TestResult testResult;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestResultRevisionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private JsonNode resultData;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id")
    private ClinicalFormTemplateVersion templateVersion;

    @Column(name = "amendment_reason", length = 1000)
    private String amendmentReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by")
    private StaffInfo enteredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by")
    private StaffInfo signedBy;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;
}
