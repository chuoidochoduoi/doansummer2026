package vn.edu.fpt.cares.dto.membership;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record MembershipPaymentRequest(
        @NotNull UUID invoiceId,
        UUID patientProfileId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã PIN phải gồm đúng 6 chữ số") String pin,
        Boolean useBenefit,
        @DecimalMin(value = "1") BigDecimal amount,
        @NotBlank String idempotencyKey) {}
