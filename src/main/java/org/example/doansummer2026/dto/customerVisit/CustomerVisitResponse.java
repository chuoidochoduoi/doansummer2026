package org.example.doansummer2026.dto.customerVisit;

import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerVisitResponse(
        UUID visitId,
        UUID customerId,
        String patientCode,       // Ma benh nhan (phone)
        String customerName,
        UUID appointmentId,
        UUID invoiceId,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        VisitStatus status,
        LocalDateTime createdAt
) {
    public static CustomerVisitResponse from(CustomerVisit v) {
        UUID customerId = v.getCustomer() != null ? v.getCustomer().getProfileId() : null;
        String customerName = v.getCustomer() != null ? v.getCustomer().getFullName() : null;
        String patientCode = v.getCustomer() != null ? v.getCustomer().getPatientCode() : null;

        // Lay thong tin guest tu appointment neu la khach vang lai
        if (v.getAppointment() != null && Boolean.TRUE.equals(v.getAppointment().getIsGuest())) {
            customerName = v.getAppointment().getGuestFullName();
        }

        UUID appointmentId = v.getAppointment() != null ? v.getAppointment().getAppointmentId() : null;
        return new CustomerVisitResponse(v.getVisitId(), customerId, patientCode, customerName, appointmentId,
                null, v.getCheckInTime(), v.getCheckOutTime(), v.getStatus(), v.getCreatedAt());
    }

    public static CustomerVisitResponse from(CustomerVisit v, UUID invoiceId) {
        UUID customerId = v.getCustomer() != null ? v.getCustomer().getProfileId() : null;
        String customerName = v.getCustomer() != null ? v.getCustomer().getFullName() : null;
        String patientCode = v.getCustomer() != null ? v.getCustomer().getPatientCode() : null;

        // Lay thong tin guest tu appointment neu la khach vang lai
        if (v.getAppointment() != null && Boolean.TRUE.equals(v.getAppointment().getIsGuest())) {
            customerName = v.getAppointment().getGuestFullName();
        }

        UUID appointmentId = v.getAppointment() != null ? v.getAppointment().getAppointmentId() : null;
        return new CustomerVisitResponse(v.getVisitId(), customerId, patientCode, customerName, appointmentId,
                invoiceId, v.getCheckInTime(), v.getCheckOutTime(), v.getStatus(), v.getCreatedAt());
    }
}

