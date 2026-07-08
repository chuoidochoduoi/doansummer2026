package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.dto.customerVisit.CustomerVisitResponse;
import org.example.doansummer2026.enums.VisitStatus;

import java.util.UUID;

/**
 * Phan hoi sau khi check-in truc tiep cho khach vang lai.
 * - Invoice duoc tao kem theo, QueueTicket se duoc tao khi thanh toan.
 */
public record GuestCheckInResponse(
        UUID visitId,
        String guestFullName,
        String guestPhone,
        UUID invoiceId,
        VisitStatus status,
        CustomerVisitResponse visit
) {
    public static GuestCheckInResponse from(
            org.example.doansummer2026.model.CustomerVisit v,
            UUID invoiceId,
            String guestFullName,
            String guestPhone) {
        return new GuestCheckInResponse(
                v.getVisitId(),
                guestFullName,
                guestPhone,
                invoiceId,
                v.getStatus(),
                CustomerVisitResponse.from(v)
        );
    }
}