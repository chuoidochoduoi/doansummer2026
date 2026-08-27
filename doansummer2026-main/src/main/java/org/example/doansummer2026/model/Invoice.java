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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.doansummer2026.enums.InvoiceStatus;

/**
 * Hoa don dich vu y te.
 * - invoiceCode: ma hoa don hien thi (VD: "INV-20260624-0001"), unique.
 * - subtotal: tong tien hang (sum InvoiceItem.unitPrice * quantity).
 * - discount: giam gia (voucher, bao hiem...). Co the am neu la phu thu.
 * - totalAmount = subtotal - discount. Co the khac voi tong cac transaction da thanh toan (neu con no).
 * - status: flow PENDING -> PAID (hoac CANCELLED).
 * - Quan he:
 *   - customer: Profile (bat buoc).
 *   - visit: CustomerVisit (optional - neu hoa don cho 1 lan kham cu the).
 *   - medicalRecord: MedicalRecord (optional - lien ket voi ho so benh an neu co).
 *   - items: 1-n InvoiceItem (owning side o day).
 *   - transactions: 1-n Transaction (owning side o Transaction).
 */
@Entity
@Table(name = "invoice")
@SQLDelete(sql = "UPDATE invoice SET deleted = true WHERE invoice_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id")
    private UUID invoiceId;

    @NotNull
    @Size(max = 30)
    @Column(name = "invoice_code", nullable = false, length = 30, unique = true)
    private String invoiceCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Profile customer;

    /** Hoa don cho 1 lan kham cu the (optional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private CustomerVisit visit;

    /** Lien ket voi ho so benh an (optional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @NotNull
    @Column(name = "issue_date", nullable = false)
    @Builder.Default
    private LocalDate issueDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull
    @PositiveOrZero
    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @NotNull
    @Column(name = "discount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "tax", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private StaffInfo issuedBy;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY,
            cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    }




