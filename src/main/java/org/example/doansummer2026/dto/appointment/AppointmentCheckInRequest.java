package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Yeu cau check-in tu appointment.
 * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
 * - serviceIds (optional): Khi muon thay doi cac dich vu da chon khi dat lich.
 */
public record AppointmentCheckInRequest(
        @NotNull UUID appointmentId,
        Set<UUID> serviceIds,
        @NotNull UUID issuedById
) {}