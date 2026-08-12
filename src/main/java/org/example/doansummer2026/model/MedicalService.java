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

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.Gender;

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
    name = "MedicalService.withDepartmentAndSpecialization",
    attributeNodes = {
        @NamedAttributeNode("department"),
        @NamedAttributeNode("requiredSpecialization")
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
    @Column(name = "department_type", nullable = false, length = 30)
    private DepartmentType departmentType;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.DRAFT;

    /** Chi ap dung cho LAB_TEST. true = xet nghiem tai cho (nhanh), false = gui lab tap trung. */
    @Column(name = "is_point_of_care", nullable = false)
    @Builder.Default
    private Boolean isPointOfCare = false;

    /** Thoi gian thuc hien uoc tinh, dung de can bang tai queue. */
    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 15;

    /** 0 = co the lam sau, 1 = binh thuong, 2 = uu tien som. */
    @Column(name = "workflow_priority")
    @Builder.Default
    private Integer workflowPriority = 1;

    @Column(name = "requires_doctor_order")
    @Builder.Default
    private Boolean requiresDoctorOrder = false;

    @Column(name = "requires_return_to_doctor")
    @Builder.Default
    private Boolean requiresReturnToDoctor = false;

    /** Dịch vụ cần tiếp nhận mẫu vật trước khi thực hiện (máu, nước tiểu, ...). */
    @Column(name = "requires_specimen", nullable = false)
    @Builder.Default
    private Boolean requiresSpecimen = false;

    @Column(name = "result_wait_minutes")
    @Builder.Default
    private Integer resultWaitMinutes = 0;

    /** Cho phep benh nhan tu dat; null duoc xem la true de tuong thich du lieu cu. */
    @Column(name = "allow_customer_booking")
    @Builder.Default
    private Boolean allowCustomerBooking = true;

    @Column(name = "minimum_age")
    private Integer minimumAge;

    @Column(name = "maximum_age")
    private Integer maximumAge;

    /** null = moi gioi tinh. */
    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_gender", length = 10)
    private Gender allowedGender;


    /** Khoa thuc hien mac dinh (nullable - mot so dich vu khong gan khoa cu the). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Chuyen khoa hien thi can thiet de thuc hien dich vu (null neu khong yeu cau). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_specialization_id")
    private Specialization requiredSpecialization;

    /** Năng lực cần có ở phòng thực hiện; thay cho liên kết cứng theo loại phòng. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_capability_id")
    private ServiceCapability requiredCapability;

    /** Ma dich vu (duy nhat, dung cho hoa don snapshot). */
    @NotBlank
    @Size(max = 20)
    @Column(name = "service_code", nullable = false, unique = true, length = 20)
    private String serviceCode;
}
