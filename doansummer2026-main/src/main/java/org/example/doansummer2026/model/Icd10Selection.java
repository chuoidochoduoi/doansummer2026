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
 * Lua chon ICD-10 trong ho so benh an.
 * Moi quan he nhiều-nhiều giữa MedicalRecord và Icd10Code.
 * Luu them thong tin lien quan đến lan chuan doan.
 */
@Entity
@Table(name = "icd_10_selections",
        indexes = {
                @jakarta.persistence.Index(name = "idx_icd_selection_record", columnList = "record_id"),
                @jakarta.persistence.Index(name = "idx_icd_selection_code", columnList = "code")
        })
@SQLDelete(sql = "UPDATE icd_10_selections SET deleted = true WHERE selection_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Icd10Selection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "selection_id")
    private UUID selectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @NotBlank
    @Column(name = "code", length = 10, nullable = false)
    private String code;

    @Column(name = "code_name", length = 255)
    private String codeName;

    /**
     * Ghi chu them cua bac si (optional).
     */
    @Column(columnDefinition = "TEXT")
    private String note;
}