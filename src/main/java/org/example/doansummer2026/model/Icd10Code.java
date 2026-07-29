package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * ICD-10 Code - Danh muc cac loai benh theo chuan ICD-10.
 * - code: Ma ICD-10 (vi du: A00, B95.9, N20.0)
 * - name: Ten benh (tieng Viet)
 * - description: Mo ta chi tiet (optional)
 * - category: Nhom benh (optional, vi du: Nhiem truyen nhiễm, Huyet hoc, ...)
 */
@Entity
@Table(name = "icd_10_codes")
@SQLDelete(sql = "UPDATE icd_10_codes SET deleted = true WHERE code = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Icd10Code {

    @Id
    @Size(max = 10)
    @Column(name = "code", length = 10)
    private String code;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100)
    @Column(length = 100)
    private String category;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}