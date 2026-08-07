package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.example.doansummer2026.enums.Gender;


/**
 * Yeu cau tao appointment cho khach khong dang nhap.
 * - Thong tin khach duoc nhap truc tiep (guestFullName, guestPhone, ...)
 * - isGuest se duoc server tu dong set true
 */
public record AppointmentGuestCreateRequest(
        @NotBlank @Size(max = 100) String guestFullName,
        @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "So dien thoai khong hop le (VN)") String guestPhone,
        @jakarta.validation.constraints.Email String guestEmail,
        @Size(max = 255) String guestAddress,
        Integer guestAge,
        Gender guestGender,
        @NotNull LocalDateTime scheduledAt,
        UUID shiftId,
        Set<UUID> serviceIds
) {}



