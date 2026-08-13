package org.example.doansummer2026.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.BloodType;

import java.time.LocalDate;
import java.util.List;

public record ProfileUpdateRequest(
        @Size(min = 2, max = 100, message = "Họ tên phải có từ 2 đến 100 ký tự")
        @Pattern(regexp = ".*\\S.*", message = "Họ tên không được để trống")
        @Pattern(regexp = "^(?!.*\\p{N}).*$", message = "Họ tên không được chứa chữ số") String fullName,
        @Past LocalDate dateOfBirth,
        @Pattern(regexp = "(?i)^(MALE|FEMALE)$", message = "Giới tính chỉ nhận MALE hoặc FEMALE") String gender,
        @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ") String phone,
        @Email(message = "Email không hợp lệ") @Size(max = 255) String email,
        @Size(max = 255) String address,
        BloodType bloodType,
        @Size(max = 50) String insuranceId,
        @jakarta.validation.constraints.Min(30) @jakarta.validation.constraints.Max(250) Integer height,
        @jakarta.validation.constraints.Min(2) @jakarta.validation.constraints.Max(500) Integer weight,
        @Size(max = 20, message = "Danh sách dị ứng không được vượt quá 20 mục")
        List<@Size(max = 100) String> allergies
) {}



