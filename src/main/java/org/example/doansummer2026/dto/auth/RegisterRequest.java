package org.example.doansummer2026.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.example.doansummer2026.enums.Gender;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$|^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email hoac so dien thoai khong hop le") String identifier,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank String fullName,
        LocalDate dob,
        Gender gender,
        @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "So dien thoai khong hop le (VN)") String phone,
        @jakarta.validation.constraints.Email String email,
        String address
) {}
