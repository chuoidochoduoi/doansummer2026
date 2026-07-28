package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotNull;
import org.example.doansummer2026.enums.TimeSlot;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AppointmentCreateRequest(
        @NotNull UUID customerId,
        @NotNull LocalDateTime scheduledAt,
        String cancelReason,
        TimeSlot timeSlot,
        Set<UUID> serviceIds
) {}



