package org.example.doansummer2026.dto.appointment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GroupAppointmentCreateRequest(
        @NotNull(message = "Vui lòng chọn thời gian khám") LocalDateTime scheduledAt,
        @NotNull(message = "Vui lòng chọn ca khám") UUID shiftId,
        @NotEmpty(message = "Vui lòng chọn người được khám")
        @Size(min = 2, message = "Đặt lịch nhóm cần ít nhất hai người")
        List<@Valid MemberBooking> members
) {
    public record MemberBooking(
            @NotNull(message = "Thiếu hồ sơ người được khám") UUID patientProfileId,
            @NotEmpty(message = "Mỗi người cần chọn ít nhất một dịch vụ") List<UUID> serviceIds
    ) {
    }
}
