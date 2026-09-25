package vn.edu.fpt.cares.dto.membership;

import jakarta.validation.constraints.*;
import vn.edu.fpt.cares.enums.PaymentMethod;
import java.math.BigDecimal;

public record MembershipTopUpRequest(
        @NotNull @DecimalMin(value = "1") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank String idempotencyKey) {}
