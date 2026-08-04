package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.UUID;

@Entity
@Table(name = "medicine_catalog", indexes = @Index(name = "idx_medicine_catalog_name", columnList = "name"))
@SQLDelete(sql = "UPDATE medicine_catalog SET deleted = true WHERE medicine_id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicineCatalog {
    @Id
    @Column(name = "medicine_id", nullable = false)
    private UUID medicineId;

    @Column(name = "medicine_code", nullable = false, unique = true, length = 50)
    private String medicineCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "active_ingredient", length = 255)
    private String activeIngredient;

    @Column(name = "default_unit", length = 30)
    private String defaultUnit;

    @Column(name = "default_usage", length = 500)
    private String defaultUsage;

    @Column(name = "default_frequency_per_day")
    private Integer defaultFrequencyPerDay;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    @PrePersist
    void initializeId() {
        if (medicineId == null) medicineId = UUID.randomUUID();
    }
}
