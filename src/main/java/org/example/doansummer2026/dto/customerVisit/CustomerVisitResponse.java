package org.example.doansummer2026.dto.customerVisit;

import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerVisitResponse(
        UUID visitId,
        UUID customerId,
        String patientCode,
        String customerName,
        String patientPhone,
        UUID appointmentId,
        UUID invoiceId,
        String serviceSummary,
        String invoiceStatus,
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
        return new CustomerVisitResponse(v.getVisitId(), customerId, patientCode, customerName,
                v.getCustomer() != null ? v.getCustomer().getPhone() : null, appointmentId,
                null, null, null, v.getCheckInTime(), v.getCheckOutTime(), v.getStatus(), v.getCreatedAt());
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
        return new CustomerVisitResponse(v.getVisitId(), customerId, patientCode, customerName,
                v.getCustomer() != null ? v.getCustomer().getPhone() : null, appointmentId,
                invoiceId, null, null, v.getCheckInTime(), v.getCheckOutTime(), v.getStatus(), v.getCreatedAt());
    }

    /** Thông tin tối thiểu để lễ tân quản lý từng lượt khám mà không phải gọi N API cho mỗi dòng. */
    public static CustomerVisitResponse from(CustomerVisit v, Invoice invoice) {
        CustomerVisitResponse base = from(v, invoice != null ? invoice.getInvoiceId() : null);
        String serviceSummary = invoice == null || invoice.getItems() == null || invoice.getItems().isEmpty()
                ? null
                : invoice.getItems().stream()
                        .map(item -> item.getService() != null ? item.getService().getName() : item.getServiceSnapshot())
                        .filter(java.util.Objects::nonNull)
                        .reduce((first, next) -> first + ", " + next)
                        .orElse(null);
        return new CustomerVisitResponse(base.visitId(), base.customerId(), base.patientCode(),
                base.customerName(), base.patientPhone(), base.appointmentId(), base.invoiceId(),
                serviceSummary, invoice != null && invoice.getStatus() != null ? invoice.getStatus().name() : null,
                base.checkInTime(), base.checkOutTime(), base.status(), base.createdAt());
    }
}
