package org.example.doansummer2026.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.SystemRole;

import java.time.LocalDate;
import java.util.UUID;

public record StaffCreateRequest(
        // Account
        @NotBlank @Size(min = 4, max = 50) String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        // Profile
        @NotBlank @Size(min = 2, max = 100)
        @Pattern(regexp = "^(?!.*\\p{N}).*$", message = "Họ tên không được chứa chữ số") String fullName,
        @NotBlank @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ") String phone,
        @NotBlank @Email String email,
        @NotNull @Past(message = "Ngày sinh phải là ngày trong quá khứ") LocalDate dateOfBirth,
        @NotBlank @Pattern(regexp = "(?i)^(MALE|FEMALE)$", message = "Giới tính chỉ nhận MALE hoặc FEMALE") String gender,
        @Size(max = 255) String address,
        // StaffInfo
        // NOTE: departmentId da duoc xoa - chi dung head_doctor_id o Department
        // specializationId: bat buoc cho DOCTOR; Kham tong quat cung la mot chuyen khoa phuc vu
        UUID specializationId,
        @NotNull SystemRole systemRole,
        @Size(max = 20) String nationalId,
        @Size(max = 30) String bankAccount,
        @Size(max = 100) String highestDegree,
        @Size(max = 200) String university,
        @Size(max = 50) String licenseNumber
) {}
