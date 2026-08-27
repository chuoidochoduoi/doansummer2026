package org.example.doansummer2026.dto.clinic;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ClinicInformationRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 150, message = "Tên hiển thị không được vượt quá 150 ký tự")
        String clinicName,

        @NotBlank(message = "Tên pháp lý không được để trống")
        @Size(max = 200, message = "Tên pháp lý không được vượt quá 200 ký tự")
        String legalName,

        @NotBlank(message = "Mã số thuế không được để trống")
        @Pattern(regexp = "^\\d{10}(?:-\\d{3})?$", message = "Mã số thuế phải gồm 10 số hoặc có dạng 10 số-3 số")
        String taxCode,

        @Size(max = 100, message = "Số giấy phép không được vượt quá 100 ký tự")
        String operatingLicense,

        @Size(max = 500, message = "Giới thiệu ngắn không được vượt quá 500 ký tự")
        String shortDescription,

        @NotBlank(message = "Email hỗ trợ không được để trống")
        @Email(message = "Email hỗ trợ không đúng định dạng")
        @Size(max = 150, message = "Email hỗ trợ không được vượt quá 150 ký tự")
        String supportEmail,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^[+0-9][0-9 .()-]{7,29}$", message = "Số điện thoại không đúng định dạng")
        String phone,

        @NotBlank(message = "Địa chỉ phòng khám không được để trống")
        @Size(max = 300, message = "Địa chỉ không được vượt quá 300 ký tự")
        String address,

        @Pattern(regexp = "^$|^https?://.+$", message = "Website phải bắt đầu bằng http:// hoặc https://")
        @Size(max = 500) String websiteUrl,

        @Pattern(regexp = "^$|^https?://.+$", message = "Liên kết Facebook phải bắt đầu bằng http:// hoặc https://")
        @Size(max = 500) String facebookUrl,

        @Pattern(regexp = "^$|^https?://.+$", message = "Liên kết YouTube phải bắt đầu bằng http:// hoặc https://")
        @Size(max = 500) String youtubeUrl,

        @Pattern(regexp = "^$|^https?://.+$", message = "Liên kết Zalo phải bắt đầu bằng http:// hoặc https://")
        @Size(max = 500) String zaloUrl,

        @DecimalMin(value = "-90", message = "Vĩ độ phải từ -90 đến 90")
        @DecimalMax(value = "90", message = "Vĩ độ phải từ -90 đến 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "Kinh độ phải từ -180 đến 180")
        @DecimalMax(value = "180", message = "Kinh độ phải từ -180 đến 180")
        BigDecimal longitude
) {}
