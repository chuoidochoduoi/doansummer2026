package org.example.doansummer2026.dto.membership;

import org.example.doansummer2026.model.MembershipCard;
import org.example.doansummer2026.model.MembershipCardLedger;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MembershipTopUpResponse(String referenceCode, String cardCode, String ownerName,
        BigDecimal amount, BigDecimal balance, String paymentMethod, LocalDateTime createdAt,
        String status, LocalDateTime benefitStartsAt, LocalDateTime benefitExpiresAt, String cashierName) {
    public static MembershipTopUpResponse from(MembershipCard card, MembershipCardLedger ledger) {
        LocalDateTime startsAt = card.getBenefitStartsAt() != null ? card.getBenefitStartsAt()
                : card.getActivatedAt() != null && card.getBenefitExpiresAt() != null
                    ? card.getActivatedAt().toLocalDate().plusDays(1).atStartOfDay() : null;
        return new MembershipTopUpResponse(ledger.getReferenceCode(), card.getCardCode(),
                card.getOwnerProfile().getFullName(), ledger.getAmount(), ledger.getBalanceAfter(),
                ledger.getSourcePaymentMethod().name(), ledger.getCreatedAt(), card.getStatus().name(),
                startsAt, card.getBenefitExpiresAt(),
                ledger.getPerformedBy() != null && ledger.getPerformedBy().getProfile() != null
                        ? ledger.getPerformedBy().getProfile().getFullName() : null);
    }
}
