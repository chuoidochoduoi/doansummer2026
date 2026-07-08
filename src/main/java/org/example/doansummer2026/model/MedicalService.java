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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.doansummer2026.enums.ServiceType;

/**
 * Dich vu y te (kham benh, xet nghiem, CDHA, ...).
 * - price: gia hien tai (VND), co the doi theo thoi gian (audit qua InvoiceItem snapshot).
 * - isPointOfCare: chi ap dung cho LAB_TEST - xet nghiem tai cho (nhanh) hay gui ve lab tap trung.
 * - durationMinutes: thoi gian uoc tinh, dung cho queue scheduling.
 * - KHONG co 2 field mappedBy "appointments" va "visits" de tranh duplicate reference (truy van nguoc bang JPQL khi can).
 */
@Entity
@Table(name = "medical_service")
@SQLDelete(sql = "UPDATE medical_service SET deleted = true WHERE service_id = ?")
@SQLRestriction("deleted = false")
@NamedEntityGraph(
    name = "MedicalService.withDepartment",
    attributeNodes = {
        @NamedAttributeNode("department")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalService extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_id")
    private UUID serviceId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    private ServiceType serviceType;

    @PositiveOrZero
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Chi ap dung cho LAB_TEST. true = xet nghiem tai cho (nhanh), false = gui lab tap trung. */
    @Column(name = "is_point_of_care", nullable = false)
    @Builder.Default
    private Boolean isPointOfCare = false;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    /** Khoa thuc hien mac dinh (nullable - mot so dich vu khong gan khoa cu the). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "required_specialization_id")
//    private Specialization requiredSpecialization;
}
