package org.example.doansummer2026.dto.customerVisit;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.example.doansummer2026.enums.Gender;

/**
 * Yeu cau tao CustomerVisit moi.
 * - Neu customerId null: tao Profile moi cho khach vang lai.
 * - serviceIds: cac dich vu se tao QueueTicket sau khi thanh toan.
 */
public record CustomerVisitCreateRequest(
        UUID customerId,  // null cho khach vang lai
        UUID appointmentId,
        @NotNull List<UUID> serviceIds,  // nhieu dich vu
        @NotNull UUID issuedById,
        // Thong tin khach vang lai (can khi customerId null)
        String guestFullName,
        String guestPhone,
        String guestAddress,
        LocalDate guestDateOfBirth,
        Gender guestGender
) {}