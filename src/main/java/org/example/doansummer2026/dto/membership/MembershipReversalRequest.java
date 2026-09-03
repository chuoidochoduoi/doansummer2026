package org.example.doansummer2026.dto.membership;

import jakarta.validation.constraints.NotBlank;

public record MembershipReversalRequest(@NotBlank String reason, @NotBlank String idempotencyKey) {}
