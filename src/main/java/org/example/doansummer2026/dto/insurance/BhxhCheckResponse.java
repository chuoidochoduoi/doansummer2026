package org.example.doansummer2026.dto.insurance;

import java.util.UUID;

public record BhxhCheckResponse(
        boolean isValid,
        String message,
        UUID insuranceId,
        String insuranceName,
        String fullName,
        String dateOfBirth
) {
}
