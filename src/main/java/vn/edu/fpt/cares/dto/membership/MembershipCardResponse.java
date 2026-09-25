package vn.edu.fpt.cares.dto.membership;

import vn.edu.fpt.cares.model.MembershipCard;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MembershipCardResponse(UUID cardId, String cardCode, UUID ownerProfileId,
        String ownerName, String status, BigDecimal balance, BigDecimal benefitPercent,
        LocalDateTime activatedAt, LocalDateTime benefitStartsAt, LocalDateTime benefitExpiresAt,
        boolean benefitActive) {
    public static MembershipCardResponse from(MembershipCard c) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime startsAt = c.getBenefitStartsAt() != null ? c.getBenefitStartsAt()
                : c.getActivatedAt() != null && c.getBenefitExpiresAt() != null
                    ? c.getActivatedAt().toLocalDate().plusDays(1).atStartOfDay() : null;
        boolean active = startsAt != null && !now.isBefore(startsAt)
                && c.getBenefitExpiresAt() != null && now.isBefore(c.getBenefitExpiresAt());
        return new MembershipCardResponse(c.getCardId(), c.getCardCode(), c.getOwnerProfile().getProfileId(),
                c.getOwnerProfile().getFullName(), c.getStatus().name(), c.getBalance(), c.getBenefitPercent(),
                c.getActivatedAt(), startsAt, c.getBenefitExpiresAt(), active);
    }
}
