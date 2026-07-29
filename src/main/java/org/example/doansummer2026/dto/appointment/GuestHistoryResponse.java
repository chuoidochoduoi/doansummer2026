package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thong tin lich su khám cua guest.
 */
public record GuestHistoryResponse(
        UUID appointmentId,
        String guestFullName,
        String guestPhone,
        String guestAddress,
        Integer guestAge,
        Gender guestGender,
        LocalDateTime scheduledAt,
        AppointmentStatus status
) {
    public static GuestHistoryResponse from(Appointment a) {
        return new GuestHistoryResponse(
                a.getAppointmentId(),
                a.getGuestFullName(),
                a.getGuestPhone(),
                a.getGuestAddress(),
                a.getGuestAge(),
                a.getGuestGender(),
                a.getScheduledAt(),
                a.getStatus()
        );
    }
}