package vn.edu.fpt.cares.dto.vitalsigns;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record VitalSignsCreateRequest(
        @NotNull UUID medicalRecordId,
        String bloodPressure,
        Integer heartRate,
        BigDecimal temperature,
        BigDecimal weight,
        BigDecimal height,
        UUID recordedById
) {}




