package org.example.doansummer2026.dto.membership;

import jakarta.validation.constraints.*;
import org.example.doansummer2026.enums.PaymentMethod;
import java.math.BigDecimal;

public record MembershipTopUpRequest(
        @NotNull @DecimalMin(value = "1") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank String idempotencyKey) {}
