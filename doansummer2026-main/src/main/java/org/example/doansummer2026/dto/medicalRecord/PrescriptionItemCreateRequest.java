package org.example.doansummer2026.dto.medicalRecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PrescriptionItemCreateRequest(
        @NotBlank String medicineName,
        @NotNull @Positive Integer quantity,
        String unit,
        String note,
        Integer frequencyPerDay
) {}



