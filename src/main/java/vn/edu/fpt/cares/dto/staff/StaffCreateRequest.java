package vn.edu.fpt.cares.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.cares.enums.SystemRole;

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
        @Size(max = 2000000, message = "Dữ liệu ảnh đại diện quá lớn") String avatarUrl,
        // StaffInfo
        // Phong chuyen mon duoc gan tai man Quan ly phong, khong gan luc tao tai khoan.
        // specializationId: bat buoc cho DOCTOR; Kham tong quat cung la mot chuyen khoa phuc vu
        UUID specializationId,
        @NotNull SystemRole systemRole,
        @Size(max = 20) String nationalId,
        @Size(max = 100) String highestDegree,
        @Size(max = 200) String university,
        @Size(max = 50) String licenseNumber
) {}
