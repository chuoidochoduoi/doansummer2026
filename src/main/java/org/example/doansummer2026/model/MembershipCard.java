package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.MembershipCardStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "membership_card")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipCard extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_id") private UUID cardId;
    @Column(name = "card_code", nullable = false, unique = true, length = 24) private String cardCode;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_profile_id", nullable = false, unique = true) private Profile ownerProfile;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MembershipCardStatus status;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal balance;
    @Column(name = "pin_hash", nullable = false, length = 100) private String pinHash;
    @Column(name = "benefit_percent", nullable = false, precision = 5, scale = 2) private BigDecimal benefitPercent;
    @Column(name = "activated_at") private LocalDateTime activatedAt;
    @Column(name = "benefit_expires_at") private LocalDateTime benefitExpiresAt;
    @Version private Long version;
}
