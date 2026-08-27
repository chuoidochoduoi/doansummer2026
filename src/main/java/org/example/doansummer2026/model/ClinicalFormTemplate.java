package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.ClinicalFormContext;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "clinical_form_template", uniqueConstraints = @UniqueConstraint(name = "uk_clinical_template_code", columnNames = "code"))
@SQLDelete(sql = "UPDATE clinical_form_template SET deleted = true WHERE template_id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClinicalFormTemplate extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClinicalFormContext context;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
