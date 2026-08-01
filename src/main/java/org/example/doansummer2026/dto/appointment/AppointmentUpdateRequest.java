package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.TimeSlot;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AppointmentUpdateRequest(
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String cancelReason,
        Set<UUID> serviceIds,
        TimeSlot timeSlot,
        String guestFullName,
        String guestPhone,
        String guestAddress,
        Integer guestAge,
        Gender guestGender
) {}



