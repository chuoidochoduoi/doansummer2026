package vn.edu.fpt.cares.dto.membership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * The existing PIN is deliberately never accepted or returned by this flow.
 * The account password proves that the signed-in customer owns the card.
 */
public record MembershipPinResetRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu tài khoản") String currentPassword,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã PIN mới phải gồm đúng 6 chữ số") String newPin,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Xác nhận mã PIN phải gồm đúng 6 chữ số") String confirmPin) {
}
