package org.example.doansummer2026.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileCreateRequest(
        @NotNull UUID accountId,
        @NotBlank @Size(max = 100) String fullName,
        @Past LocalDate dateOfBirth,
        String gender,
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "So dien thoai khong hop le (VN)") String phone,
        @NotBlank @Email String email,
        @Size(max = 255) String address
) {}



