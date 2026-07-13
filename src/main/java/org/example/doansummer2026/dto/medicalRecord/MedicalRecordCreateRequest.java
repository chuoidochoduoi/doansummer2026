package org.example.doansummer2026.dto.medicalRecord;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicalRecordCreateRequest(
        @NotNull UUID visitId,
        @NotNull UUID doctorId,
        String chiefComplaint,
        // Vital signs - tao luon khi tao medical record
        String bloodPressure,
        Integer heartRate,
        BigDecimal temperature,
        BigDecimal weight,
        BigDecimal height,
        UUID recordedById
) {}