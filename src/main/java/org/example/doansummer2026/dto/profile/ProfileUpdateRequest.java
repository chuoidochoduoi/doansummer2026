package org.example.doansummer2026.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.BloodType;

import java.time.LocalDate;
import java.util.List;

public record ProfileUpdateRequest(
        @Size(max = 100) String fullName,
        @Past LocalDate dateOfBirth,
        String gender,
        @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "So dien thoai khong hop le (VN)") String phone,
        @Email String email,
        @Size(max = 255) String address,
        BloodType bloodType,
        @Size(max = 50) String insuranceId,
        @jakarta.validation.constraints.Min(30) @jakarta.validation.constraints.Max(250) Integer height,
        @jakarta.validation.constraints.Min(2) @jakarta.validation.constraints.Max(500) Integer weight,
        List<@Size(max = 100) String> allergies
) {}



