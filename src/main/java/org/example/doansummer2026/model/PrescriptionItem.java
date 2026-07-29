package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Thuoc trong don thuoc - nhap tu do (free text).
 * 1 MedicalRecord co nhieu PrescriptionItem.
 */
@Entity
@Table(name = "prescription_item")
@SQLDelete(sql = "UPDATE prescription_item SET deleted = true WHERE prescription_item_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prescription_item_id")
    private UUID prescriptionItemId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String medicineName;

    /** So luong thuoc (goi y: vien, hop, chai,...) */
    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    /** Don vi: vien, hop, chai, goi, ... */
    @Column(length = 50)
    private String unit;

    /** Ghi chu: huong dan su dung, chu yeu... */
    @Column(columnDefinition = "TEXT")
    private String note;

    /** Tan suat su dung trong ngay (1-4 lan/ngay) */
    @Column(name = "frequency_per_day")
    private Integer frequencyPerDay;
}



