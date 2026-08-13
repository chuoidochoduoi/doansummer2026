package org.example.doansummer2026.dto.appointment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
public record AppointmentUpdateRequest(
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String cancelReason,
        Set<UUID> serviceIds,
        UUID shiftId,

        @Size(min = 2, max = 100)
        @Pattern(regexp = "^(?!.*\\p{N}).*$", message = "Họ tên không được chứa chữ số") String guestFullName,
        @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ") String guestPhone,
        @Email(message = "Email không hợp lệ") @Size(max = 255) String guestEmail,
        @Size(max = 255) String guestAddress,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ") LocalDate guestDateOfBirth,

        Integer guestAge,
        Gender guestGender
) {}
