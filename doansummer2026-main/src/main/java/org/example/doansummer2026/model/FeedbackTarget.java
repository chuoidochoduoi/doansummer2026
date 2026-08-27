package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "feedback_target", uniqueConstraints = @UniqueConstraint(
        name = "uk_feedback_target", columnNames = {"medical_record_id", "target_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackTarget {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_target_id")
    private UUID feedbackTargetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @Column(name = "target_key", nullable = false, length = 100)
    private String targetKey;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_name", nullable = false, length = 200)
    private String targetName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private StaffInfo staff;

    @Column(name = "source_record_id")
    private UUID sourceRecordId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String comment;

    @Column(name = "staff_explanation", length = 1000)
    private String staffExplanation;
}
