package org.example.doansummer2026.dto.customerVisit;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.example.doansummer2026.enums.Gender;

/**
 * Yeu cau tao CustomerVisit moi.
 * - Neu customerId null: tao Profile moi cho khach vang lai.
 * - serviceIds (optional): cac dich vu se tao QueueTicket sau khi thanh toan.
 * - issuedById (optional): neu null se lay tu staff dang dang nhap.
 */
public record CustomerVisitCreateRequest(
        UUID customerId,  // null cho khach vang lai
        UUID appointmentId,
        List<UUID> serviceIds,  // optional
        UUID issuedById,  // optional
        // Thong tin khach vang lai (can khi customerId null)
        String guestFullName,
        String guestPhone,
        String guestAddress,
        LocalDate guestDateOfBirth,
        Gender guestGender
) {}