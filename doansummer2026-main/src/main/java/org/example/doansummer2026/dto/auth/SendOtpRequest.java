package org.example.doansummer2026.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendOtpRequest(
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$|^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email hoặc số điện thoại không hợp lệ") String identifier
) {}




