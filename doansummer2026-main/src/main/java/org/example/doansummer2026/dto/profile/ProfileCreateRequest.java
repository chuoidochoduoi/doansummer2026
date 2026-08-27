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
        @NotBlank @Size(min = 2, max = 100)
        @Pattern(regexp = "^(?!.*\\p{N}).*$", message = "Họ tên không được chứa chữ số") String fullName,
        @Past LocalDate dateOfBirth,
        String gender,
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ") String phone,
        @NotBlank @Email String email,
        @Size(max = 255) String address
) {}



