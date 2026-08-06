package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotNull;


import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AppointmentCreateRequest(
        @NotNull UUID customerId,
        @NotNull LocalDateTime scheduledAt,
        String cancelReason,
        UUID shiftId,
        Set<UUID> serviceIds
) {}



