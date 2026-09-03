package org.example.doansummer2026.dto.membership;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MembershipPolicyRequest(
        @NotNull @DecimalMin("1") BigDecimal minimumTopUp,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent,
        @NotNull @Min(1) @Max(120) Integer validityMonths) {}
