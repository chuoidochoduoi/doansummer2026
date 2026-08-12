package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
public record AppointmentUpdateRequest(
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String cancelReason,
        Set<UUID> serviceIds,
        UUID shiftId,

        String guestFullName,
        String guestPhone,
        String guestEmail,
        String guestAddress,

        LocalDate guestDateOfBirth,

        Integer guestAge,
        Gender guestGender
) {}
