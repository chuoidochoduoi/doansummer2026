package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chi so sinh hieu (huyet ap, mach, nhiet do, can, cao...).
 * 1 MedicalRecord co 0..1 VitalSigns.
 */
@Entity
@Table(name = "vital_signs")
@SQLDelete(sql = "UPDATE vital_signs SET deleted = true WHERE vital_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSigns extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vital_id")
    private UUID vitalId;

    /** Owning 1-1 (FK o day). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false, unique = true)
    private MedicalRecord medicalRecord;

    /** "120/80 mmHg" */
    @Column(name = "blood_pressure", length = 30)
    private String bloodPressure;

    /** BPM */
    @Column(name = "heart_rate")
    private Integer heartRate;

    /** Do C */
    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    /** kg */
    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    /** cm */
    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private StaffInfo recordedBy;
}



