package org.example.doansummer2026.dto.vitalsigns;

import java.math.BigDecimal;

public record VitalSignsUpdateRequest(
        String bloodPressure,
        Integer heartRate,
        BigDecimal temperature,
        BigDecimal weight,
        BigDecimal height
) {}




