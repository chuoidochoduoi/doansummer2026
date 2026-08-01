package org.example.doansummer2026.dto.bhyt;

import java.time.LocalDate;
import java.util.UUID;

public record BhytCheckResponse(
        String cardNumber,
        String fullName,
        LocalDate dateOfBirth,
        UUID insuranceId,
        String insuranceName,
        LocalDate validFrom,
        LocalDate validTo,
        boolean isValid,
        String message
) {}
