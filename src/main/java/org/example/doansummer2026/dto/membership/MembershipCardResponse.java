package org.example.doansummer2026.dto.membership;

import org.example.doansummer2026.model.MembershipCard;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MembershipCardResponse(UUID cardId, String cardCode, UUID ownerProfileId,
        String ownerName, String status, BigDecimal balance, BigDecimal benefitPercent,
        LocalDateTime activatedAt, LocalDateTime benefitExpiresAt, boolean benefitActive) {
    public static MembershipCardResponse from(MembershipCard c) {
        boolean active = c.getBenefitExpiresAt() != null && c.getBenefitExpiresAt().isAfter(LocalDateTime.now());
        return new MembershipCardResponse(c.getCardId(), c.getCardCode(), c.getOwnerProfile().getProfileId(),
                c.getOwnerProfile().getFullName(), c.getStatus().name(), c.getBalance(), c.getBenefitPercent(),
                c.getActivatedAt(), c.getBenefitExpiresAt(), active);
    }
}
