package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "medical_service_form_template", uniqueConstraints = @UniqueConstraint(
        name = "uk_service_form_template_service", columnNames = "service_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicalServiceFormTemplate extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "binding_id")
    private UUID bindingId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ClinicalFormTemplate template;
}
