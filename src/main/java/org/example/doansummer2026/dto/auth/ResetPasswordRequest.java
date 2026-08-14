package org.example.doansummer2026.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email/Số điện thoại không được để trống")
        @Pattern(regexp = "^(\\+84|0)\\d{9,10}$|^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email hoặc số điện thoại không hợp lệ")
        String identifier,

        @NotBlank(message = "OTP không được để trống")
        String otp,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
        String newPassword
) {
}
