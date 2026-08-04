package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.example.doansummer2026.enums.MedicalRecordStatus;

/**
 * Ho so benh an - tao boi bac si khi bat dau kham.
 * Status flow: IN_PROGRESS -> COMPLETED (khoa, khong cho sua).
 * Cac quan he:
 *  - visit: 1-1 voi CustomerVisit (owning side o day)
 *  - doctor: bac si phu trach (bat buoc)
 *  - vitalSigns: 1-1 (owning)
 *  - testRequests: 1-n (owning)
 *  - icdSelections: 1-n (owning) - cac benh chuan doan theo ICD-10
 */
@Entity
@Table(name = "medical_record")
@SQLDelete(sql = "UPDATE medical_record SET deleted = true WHERE record_id = ? AND record_version = ?")
@SQLRestriction("deleted = false")
@NamedEntityGraph(
    name = "MedicalRecord.withDetails",
    attributeNodes = {
        @NamedAttributeNode("vitalSigns"),
        @NamedAttributeNode("testRequests"),
        @NamedAttributeNode("prescriptionItems"),
        @NamedAttributeNode("icdSelections"),
        @NamedAttributeNode("visit")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord extends BaseEntity {

    @jakarta.persistence.Version
    @Column(name = "record_version", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id")
    private UUID recordId;

    /** Ma so benh an (duy nhat, dinh dang: MR-YYYY-XXXXX). */
    @Size(max = 50)
    @Column(name = "record_code", unique = true, length = 50)
    private String recordCode;

    /** Owning side 1-1 voi CustomerVisit. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private CustomerVisit visit;

    /** Moi lan kham/chuyen khoa co mot ho so rieng. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_ticket_id", unique = true)
    private QueueTicket queueTicket;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private StaffInfo doctor;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "clinical_findings", columnDefinition = "TEXT")
    private String clinicalFindings;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "prescription_note", columnDefinition = "TEXT")
    private String prescriptionNote;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @Column(name = "patient_instruction", columnDefinition = "TEXT")
    private String patientInstruction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MedicalRecordStatus status = MedicalRecordStatus.IN_PROGRESS;

    /** Danh sach thuoc trong don (chua khi tao/ket luuan). */
    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY,
            cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE},
            orphanRemoval = true)
    @Builder.Default
    private Set<PrescriptionItem> prescriptionItems = new LinkedHashSet<>();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Owning 1-1 voi VitalSigns. */
    @OneToOne(fetch = FetchType.LAZY, cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @JoinColumn(name = "vital_signs_id", unique = true)
    private VitalSigns vitalSigns;

    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY,
            cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE},
            orphanRemoval = true)
    @Builder.Default
    private Set<TestRequest> testRequests = new LinkedHashSet<>();

    /** Danh sach cac benh chuan doan theo ICD-10. */
    @OneToMany(mappedBy = "medicalRecord", fetch = FetchType.LAZY,
            cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE},
            orphanRemoval = true)
    @Builder.Default
    private Set<Icd10Selection> icdSelections = new LinkedHashSet<>();

    /** Diem danh gia tu benh nhan (1-5 sao). */
    @Column(name = "rating_score")
    private Integer ratingScore;

    @Column(name = "rated_at")
    private LocalDateTime ratedAt;

    @Column(name = "rating_comment", length = 500) private String ratingComment;
    @Column(name = "doctor_rating") private Integer doctorRating;
    @Column(name = "waiting_rating") private Integer waitingRating;
    @Column(name = "staff_rating") private Integer staffRating;
    @Column(name = "contact_requested") @Builder.Default private Boolean contactRequested = false;
    @Column(name = "feedback_status", length = 20) private String feedbackStatus;
    @Column(name = "manager_response", length = 1000) private String managerResponse;
    @Column(name = "internal_note", length = 1000) private String internalNote;
    @Column(name = "doctor_explanation", length = 1000) private String doctorExplanation;
    @Column(name = "responded_at") private LocalDateTime respondedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responded_by") private StaffInfo respondedBy;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "nursing_updated_by") private StaffInfo nursingUpdatedBy;
    @Column(name = "nursing_updated_at") private LocalDateTime nursingUpdatedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doctor_confirmed_by") private StaffInfo doctorConfirmedBy;
    @Column(name = "doctor_confirmed_at") private LocalDateTime doctorConfirmedAt;

    @OneToMany(mappedBy = "medicalRecord", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FeedbackTarget> feedbackTargets = new LinkedHashSet<>();

    /** Ghi chu tai kham */
    @Column(name = "follow_up_note", columnDefinition = "TEXT")
    private String followUpNote;

    /** Ngay tai kham du kien */
    @Column(name = "follow_up_date")
    private java.time.LocalDate followUpDate;

    /** Lien ket voi lich hen da duoc tao tu yeu cau tai kham nay */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_appointment_id")
    private Appointment followUpAppointment;
}
