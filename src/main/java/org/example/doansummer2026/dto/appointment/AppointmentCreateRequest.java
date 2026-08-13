package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AppointmentCreateRequest(
        @NotNull UUID customerId,
        @NotNull @FutureOrPresent(message = "Thời gian đặt lịch phải từ hiện tại trở đi") LocalDateTime scheduledAt,
        String cancelReason,
        UUID shiftId,
        Set<UUID> serviceIds
) {}



