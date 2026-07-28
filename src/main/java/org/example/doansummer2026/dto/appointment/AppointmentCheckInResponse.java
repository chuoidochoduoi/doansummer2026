package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.dto.customerVisit.CustomerVisitResponse;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.model.Appointment;

import java.util.UUID;

/**
 * Phan hoi sau khi check-in tu appointment.
 * - Invoice duoc tao kem theo, QueueTicket se duoc tao khi thanh toan.
 */
public record AppointmentCheckInResponse(
        UUID appointmentId,
        AppointmentStatus status,
        UUID invoiceId,
        CustomerVisitResponse visit
) {
    public static AppointmentCheckInResponse from(Appointment a, org.example.doansummer2026.model.CustomerVisit v, UUID invoiceId) {
        return new AppointmentCheckInResponse(
                a.getAppointmentId(),
                a.getStatus(),
                invoiceId,
                CustomerVisitResponse.from(v)
        );
    }
}



