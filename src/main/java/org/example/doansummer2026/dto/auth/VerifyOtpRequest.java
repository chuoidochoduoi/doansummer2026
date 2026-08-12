package org.example.doansummer2026.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Ma OTP khong hop le (phai 6 so)") String otp
) {}
