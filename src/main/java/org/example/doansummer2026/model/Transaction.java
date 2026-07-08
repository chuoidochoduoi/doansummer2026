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
import java.time.LocalDateTime;
import java.util.UUID;
import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.enums.TransactionStatus;

/**
 * Giao dich thanh toan - 1 hoa don co the co nhieu giao dich (tra gop, BH + tien mat...).
 * - transactionCode: ma tham chieu (VD: gateway transaction id, so phieu thu...).
 * - amount: so tien GD nay.
 * - method: PaymentMethod.
 * - status: TransactionStatus.
 * - paidAt: thoi diem thanh toan thanh cong (null neu PENDING/FAILED).
 */
@Entity
@Table(name = "payment_transaction")
@SQLDelete(sql = "UPDATE payment_transaction SET deleted = true WHERE transaction_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotNull
    @Size(max = 100)
    @Column(name = "transaction_code", nullable = false, length = 100, unique = true)
    private String transactionCode;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Size(max = 100)
    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private StaffInfo receivedBy;
}
