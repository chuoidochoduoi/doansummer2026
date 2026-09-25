package vn.edu.fpt.cares.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.fpt.cares.common.BaseEntity;
import vn.edu.fpt.cares.enums.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "membership_card_ledger")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipCardLedger extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ledger_id") private UUID ledgerId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false) private MembershipCard card;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MembershipLedgerType type;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2) private BigDecimal balanceBefore;
    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAfter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_profile_id") private Profile patientProfile;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "performed_by") private StaffInfo performedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_transaction_id") private Transaction paymentTransaction;
    @Enumerated(EnumType.STRING) @Column(name = "source_payment_method", length = 30) private PaymentMethod sourcePaymentMethod;
    @Column(name = "idempotency_key", unique = true, length = 100) private String idempotencyKey;
    @Column(name = "reference_code", unique = true, length = 40) private String referenceCode;
    @Column(name = "benefit_discount", nullable = false, precision = 18, scale = 2)
    @Builder.Default private BigDecimal benefitDiscount = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reversed_ledger_id")
    private MembershipCardLedger reversedLedger;
    @Column(columnDefinition = "TEXT") private String reason;
}
