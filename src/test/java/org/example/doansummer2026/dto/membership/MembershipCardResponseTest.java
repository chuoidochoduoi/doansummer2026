package org.example.doansummer2026.dto.membership;

import org.example.doansummer2026.enums.MembershipCardStatus;
import org.example.doansummer2026.model.MembershipCard;
import org.example.doansummer2026.model.Profile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MembershipCardResponseTest {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private LocalDateTime now() {
        return LocalDateTime.now(CLINIC_ZONE);
    }

    private MembershipCard card() {
        return MembershipCard.builder()
                .cardId(UUID.randomUUID())
                .cardCode("CS-001")
                .ownerProfile(Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Thị Ánh").build())
                .status(MembershipCardStatus.ACTIVE)
                .balance(new BigDecimal("1000000"))
                .benefitPercent(new BigDecimal("15"))
                .build();
    }

    @Test
    void explicitBenefitPeriodIsActiveOnlyInsideItsBounds() {
        MembershipCard current = card();
        current.setBenefitStartsAt(now().minusMinutes(1));
        current.setBenefitExpiresAt(now().plusMinutes(1));
        assertTrue(MembershipCardResponse.from(current).benefitActive());
        assertEquals(current.getBenefitStartsAt(), MembershipCardResponse.from(current).benefitStartsAt());

        MembershipCard future = card();
        future.setBenefitStartsAt(now().plusDays(1));
        future.setBenefitExpiresAt(now().plusMonths(1));
        assertFalse(MembershipCardResponse.from(future).benefitActive());

        MembershipCard expired = card();
        expired.setBenefitStartsAt(now().minusMonths(2));
        expired.setBenefitExpiresAt(now().minusMinutes(1));
        assertFalse(MembershipCardResponse.from(expired).benefitActive());
    }

    @Test
    void legacyCardDerivesNextDayStartOnlyWithCompletePeriod() {
        MembershipCard legacy = card();
        legacy.setActivatedAt(now().minusDays(2));
        legacy.setBenefitExpiresAt(now().plusDays(2));
        MembershipCardResponse response = MembershipCardResponse.from(legacy);
        assertEquals(legacy.getActivatedAt().toLocalDate().plusDays(1).atStartOfDay(), response.benefitStartsAt());
        assertTrue(response.benefitActive());

        MembershipCard noActivation = card();
        noActivation.setBenefitExpiresAt(now().plusDays(2));
        assertNull(MembershipCardResponse.from(noActivation).benefitStartsAt());
        assertFalse(MembershipCardResponse.from(noActivation).benefitActive());

        MembershipCard noExpiry = card();
        noExpiry.setActivatedAt(now().minusDays(2));
        assertNull(MembershipCardResponse.from(noExpiry).benefitStartsAt());
        assertFalse(MembershipCardResponse.from(noExpiry).benefitActive());
    }
}
