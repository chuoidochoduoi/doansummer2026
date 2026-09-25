package vn.edu.fpt.cares.dto.appointment;

import vn.edu.fpt.cares.dto.customervisit.CustomerVisitResponse;
import vn.edu.fpt.cares.enums.AppointmentStatus;
import vn.edu.fpt.cares.model.Appointment;

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
    public static AppointmentCheckInResponse from(Appointment a, vn.edu.fpt.cares.model.CustomerVisit v, UUID invoiceId) {
        return new AppointmentCheckInResponse(
                a.getAppointmentId(),
                a.getStatus(),
                invoiceId,
                CustomerVisitResponse.from(v)
        );
    }
}



