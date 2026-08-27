package org.example.doansummer2026.dto.customerVisit;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.AllergyStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;

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
        @Size(max = 100) String guestFullName,
        @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ") String guestPhone,
        @Size(max = 255) String guestAddress,
        @Past(message = "Ngày sinh phải là ngày trong quá khứ") LocalDate guestDateOfBirth,
        Gender guestGender,
        @Email(message = "Email không hợp lệ") @Size(max = 255) String guestEmail,
        BloodType guestBloodType,
        AllergyStatus allergyStatus,
        @Size(max = 20, message = "Danh sách dị ứng không được vượt quá 20 mục")
        List<@Size(max = 100, message = "Mỗi dị ứng không được vượt quá 100 ký tự") String> guestAllergies,
        Boolean updatePatientProfile,
        // Bao hiem y te
        UUID insuranceId
) {}



