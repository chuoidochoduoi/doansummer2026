package org.example.doansummer2026.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$|^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email hoac so dien thoai khong hop le") String identifier,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Ma OTP khong hop le (phai 6 so)") String otp
) {}



