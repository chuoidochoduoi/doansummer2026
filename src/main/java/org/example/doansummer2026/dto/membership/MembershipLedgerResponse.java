package org.example.doansummer2026.dto.membership;

import org.example.doansummer2026.model.MembershipCardLedger;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MembershipLedgerResponse(UUID ledgerId, String referenceCode, String cardCode, String type,
        BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
        UUID invoiceId, String patientName, String paymentMethod, LocalDateTime createdAt, String reason) {
    public static MembershipLedgerResponse from(MembershipCardLedger l) {
        return new MembershipLedgerResponse(l.getLedgerId(), l.getReferenceCode(), l.getCard().getCardCode(), l.getType().name(),
                l.getAmount(), l.getBalanceBefore(), l.getBalanceAfter(),
                l.getInvoice() == null ? null : l.getInvoice().getInvoiceId(),
                l.getPatientProfile() == null ? null : l.getPatientProfile().getFullName(),
                l.getSourcePaymentMethod() == null ? null : l.getSourcePaymentMethod().name(),
                l.getCreatedAt(), l.getReason());
    }
}
