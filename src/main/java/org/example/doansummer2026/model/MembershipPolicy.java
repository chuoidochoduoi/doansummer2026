package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "membership_policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipPolicy extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id")
    private UUID policyId;
    @Column(name = "minimum_top_up", nullable = false, precision = 18, scale = 2)
    private BigDecimal minimumTopUp;
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;
    @Column(name = "validity_months", nullable = false)
    private Integer validityMonths;
    @Column(nullable = false)
    private Boolean active;
}
