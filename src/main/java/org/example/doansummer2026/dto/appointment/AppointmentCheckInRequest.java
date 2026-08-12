package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.enums.Gender;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Yeu cau check-in tu appointment.
 * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
 * - serviceIds (optional): Khi muon thay doi cac dich vu da chon khi dat lich.
 * - issuedById (optional): Neu null se lay tu staff dang dang nhap.
 */
public record AppointmentCheckInRequest(
        java.util.UUID appointmentId,
        Set<UUID> serviceIds,
        UUID issuedById,
        String patientFullName,
        String patientPhone,
        String patientEmail,
        String patientAddress,
        LocalDate patientDateOfBirth,
        Integer patientAge,
        Gender patientGender
) {}



