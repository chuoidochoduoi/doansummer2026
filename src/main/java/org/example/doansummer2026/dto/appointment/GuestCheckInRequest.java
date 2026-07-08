package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

import org.example.doansummer2026.enums.Gender;

/**
 * Yeu cau check-in truc tiep cho khach vang lai (khong co appointment).
 * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
 */
public record GuestCheckInRequest(
        @NotBlank @Size(max = 100) String guestFullName,
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "So dien thoai khong hop le (VN)") String guestPhone,
        @Size(max = 255) String guestAddress,
        Integer guestAge,
        Gender guestGender,
        @NotNull Set<UUID> serviceIds,
        @NotNull UUID issuedById
) {}