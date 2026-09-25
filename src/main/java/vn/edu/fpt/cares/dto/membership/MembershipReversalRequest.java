package vn.edu.fpt.cares.dto.membership;

import jakarta.validation.constraints.NotBlank;

public record MembershipReversalRequest(@NotBlank String reason, @NotBlank String idempotencyKey) {}
