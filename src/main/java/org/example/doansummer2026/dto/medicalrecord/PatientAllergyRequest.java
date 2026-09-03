package org.example.doansummer2026.dto.medicalrecord;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.AllergyStatus;

import java.util.List;

public record PatientAllergyRequest(
        @NotNull AllergyStatus status,
        @Size(max = 20, message = "Danh sách dị ứng không được vượt quá 20 mục")
        List<@Size(max = 100, message = "Mỗi dị ứng không được vượt quá 100 ký tự") String> items
) {}
