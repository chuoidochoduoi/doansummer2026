package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AppointmentCreateRequest(
        @NotNull UUID customerId,
        @NotNull @FutureOrPresent(message = "Thoi gian dat lich phai tu hien tai tro di") LocalDateTime scheduledAt,
        String cancelReason,
        UUID shiftId,
        Set<UUID> serviceIds
) {}



