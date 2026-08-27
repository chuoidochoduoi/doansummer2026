package org.example.doansummer2026.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Mã OTP không hợp lệ (phải gồm 6 số)") String otp
) {}
