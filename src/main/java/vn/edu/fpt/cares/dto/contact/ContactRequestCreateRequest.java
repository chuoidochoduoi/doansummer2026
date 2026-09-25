package vn.edu.fpt.cares.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequestCreateRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên")
        @Size(min = 2, max = 100, message = "Họ tên phải có từ 2 đến 100 ký tự")
        @Pattern(regexp = "^(?!.*\\p{N}).*$", message = "Họ tên không được chứa chữ số")
        String fullName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại Việt Nam không hợp lệ")
        String phone,

        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
        String email,

        @NotBlank(message = "Vui lòng nhập chủ đề")
        @Size(min = 3, max = 150, message = "Chủ đề phải có từ 3 đến 150 ký tự")
        String subject,

        @NotBlank(message = "Vui lòng nhập nội dung liên hệ")
        @Size(min = 10, max = 1000, message = "Nội dung phải có từ 10 đến 1000 ký tự")
        String message
) {}
