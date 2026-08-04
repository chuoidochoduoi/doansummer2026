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
import org.example.doansummer2026.enums.TestRequestStatus;

/**
 * Yeu cau xet nghiem / CDHA do bac si chi dinh trong qua trinh kham.
 * - service: goi dich vu xet nghiem
 * - performingDepartment: khoa thuc hien (LAB, CDHA, ...)
 * - status: PENDING -> IN_PROGRESS -> COMPLETED
 * - testResult: 0..1 (1-1 voi TestResult)
 * - invoiceItem: lien ket voi InvoiceItem tu hoa don da thanh toan (traceability).
 *
 * Luong: Invoice(paid) -> TestRequest(PENDING = hang cho) -> TestResult -> TestRequest(COMPLETED).
 */
@Entity
@Table(name = "test_request")
@SQLDelete(sql = "UPDATE test_request SET deleted = true WHERE test_request_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "test_request_id")
    private UUID testRequestId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performing_department", nullable = false)
    private Department performingDepartment;

    /** Phiếu gọi số của phòng cận lâm sàng; nhiều kỹ thuật cùng phòng dùng chung một phiếu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_ticket_id")
    private QueueTicket queueTicket;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TestRequestStatus status = TestRequestStatus.PENDING;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private StaffInfo requestedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "performed_at")
    private LocalDateTime performedAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    /** Quan he 1-1 voi TestResult - owning o TestResult. */
    @OneToOne(mappedBy = "testRequest", fetch = FetchType.LAZY)
    private TestResult testResult;

    /**
     * InvoiceItem tu hoa don da thanh toan tao ra TestRequest nay.
     * Co the null neu TestRequest duoc tao thu cong (khong qua invoice).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id")
    private InvoiceItem invoiceItem;
}



