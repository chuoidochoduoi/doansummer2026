package org.example.doansummer2026.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.SystemRole;

import java.time.LocalDate;
import java.util.UUID;

public record StaffUpdateRequest(
        // NOTE: departmentId da duoc xoa - chi dung head_doctor_id o Department
        // Account & Profile
        @Size(max = 50) String username,
        @Size(min = 2, max = 100, message = "Họ tên phải có từ 2 đến 100 ký tự")
        @Pattern(regexp = ".*\\S.*", message = "Họ tên không được để trống")
        @Pattern(regexp = "^(?!.*\\p{N}).*$", message = "Họ tên không được chứa chữ số") String fullName,
        @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ") String phone,
        @Email(message = "Email không hợp lệ") @Size(max = 255) String email,
        @Past(message = "Ngày sinh phải là ngày trong quá khứ") LocalDate dateOfBirth,
        @Pattern(regexp = "(?i)^(MALE|FEMALE)$", message = "Giới tính chỉ nhận MALE hoặc FEMALE") String gender,
        @Size(max = 255) String address,

        // StaffInfo
        UUID specializationId,
        SystemRole systemRole,
        @Size(max = 20) String nationalId,
        @Size(max = 30) String bankAccount,
        @Size(max = 100) String highestDegree,
        @Size(max = 200) String university,
        @Size(max = 50) String licenseNumber
) {}
