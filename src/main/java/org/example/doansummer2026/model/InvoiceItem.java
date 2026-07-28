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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import java.util.UUID;

/**
 * Dong chi tiet cua hoa don (snapshot gia tai thoi diem xuat).
 * - serviceSnapshot: ten dich vu tai thoi diem lap hoa don (tranh phu thuoc vao doi gia sau nay).
 * - unitPrice: gia snapshot (VND).
 * - quantity: so luong.
 * - lineTotal = unitPrice * quantity.
 *
 * Luu y: Service hien tai co the doi gia -> van phai luu snapshot de lich su gia duoc toan ven.
 */
@Entity
@Table(name = "invoice_item")
@SQLDelete(sql = "UPDATE invoice_item SET deleted = true WHERE item_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** FK den MedicalService (co the null neu dich vu da xoa). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private MedicalService service;

    @NotNull
    @Size(max = 200)
    @Column(name = "service_snapshot", nullable = false, length = 200)
    private String serviceSnapshot;

    @Size(max = 50)
    @Column(name = "service_code_snapshot", length = 50)
    private String serviceCodeSnapshot;

    @NotNull
    @PositiveOrZero
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Positive
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @NotNull
    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** So tien duoc BHYT chi tra (VND). */
    @Column(name = "bhyt_fund", precision = 18, scale = 2)
    private BigDecimal bhytFund;
}



