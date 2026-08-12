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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;
import org.example.doansummer2026.enums.SpecimenStatus;
import org.example.doansummer2026.enums.SpecimenType;

/**
 * Ket qua xet nghiem - luu duong dan anh ket qua (khong con luu JSON key-value).
 * - imageUrl: duong dan luu anh ket qua xet nghiem (upload to cloud/storage).
 * - performedBy: ky thuat vien / bac si chuyen khoa nhap ket qua
 */
@Entity
@Table(name = "test_result")
@SQLDelete(sql = "UPDATE test_result SET deleted = true WHERE result_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id")
    private UUID resultId;

    /** Owning side 1-1 voi TestRequest. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_request_id", nullable = false, unique = true)
    private TestRequest testRequest;

    /** Duong dan anh ket qua xet nghiem (upload to cloud/storage). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    /** Ma mau vat (tu may quet hoac thiet bi y te). */
    @Column(name = "sample_id", length = 100)
    private String sampleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sample_type", length = 30)
    private SpecimenType sampleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sample_status", length = 30)
    private SpecimenStatus sampleStatus;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by")
    private StaffInfo collectedBy;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private StaffInfo performedBy;

    @NotNull
    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();

    /** Bac si chuyen mon ky xac nhan ket qua sau khi nhan vien luu nhap. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private StaffInfo verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}

