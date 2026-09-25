package vn.edu.fpt.cares.dto.appointment;

import vn.edu.fpt.cares.dto.customervisit.CustomerVisitResponse;
import vn.edu.fpt.cares.enums.VisitStatus;

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
            vn.edu.fpt.cares.model.CustomerVisit v,
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



