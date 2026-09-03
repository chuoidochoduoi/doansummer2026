package org.example.doansummer2026.dto.membership;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record MembershipCounterPaymentRequest(
        @NotBlank String cardCode,
        @NotNull UUID invoiceId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã PIN phải gồm đúng 6 chữ số") String pin,
        Boolean useBenefit,
        @DecimalMin(value = "1") BigDecimal amount,
        @NotBlank String idempotencyKey) {}
