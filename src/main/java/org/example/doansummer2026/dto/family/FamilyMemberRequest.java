package org.example.doansummer2026.dto.family;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.AllergyStatus;
import org.example.doansummer2026.enums.FamilyRelationship;
import org.example.doansummer2026.enums.Gender;

import java.time.LocalDate;
import java.util.List;

public record FamilyMemberRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên")
        @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
        @Pattern(regexp = "^[^0-9]*$", message = "Họ và tên không được chứa chữ số")
        String fullName,
        @NotNull(message = "Vui lòng chọn ngày sinh")
        @Past(message = "Ngày sinh phải trước ngày hiện tại")
        LocalDate dateOfBirth,
        @NotNull(message = "Vui lòng chọn giới tính") Gender gender,
        @NotNull(message = "Vui lòng chọn quan hệ") FamilyRelationship relationship,
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự") String address,
        BloodType bloodType,
        AllergyStatus allergyStatus,
        @Size(max = 20, message = "Chỉ được nhập tối đa 20 dị ứng")
        List<@Size(max = 100, message = "Mỗi dị ứng không được vượt quá 100 ký tự") String> allergies,
        @AssertTrue(message = "Vui lòng xác nhận quyền quản lý hồ sơ thành viên") Boolean managementConfirmed
) {
}
