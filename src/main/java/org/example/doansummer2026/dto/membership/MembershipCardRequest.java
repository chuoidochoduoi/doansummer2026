package org.example.doansummer2026.dto.membership;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record MembershipCardRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã PIN phải gồm đúng 6 chữ số") String pin,
        Boolean acceptedTerms) {}
