package vn.edu.fpt.cares.dto.appointment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerAppointmentCreateRequest(
        UUID patientProfileId,
        @NotNull(message = "Vui lòng chọn thời gian khám") LocalDateTime scheduledAt,
        @NotNull(message = "Vui lòng chọn ca khám") UUID shiftId,
        @NotEmpty(message = "Vui lòng chọn ít nhất một dịch vụ") List<UUID> serviceIds
) {
}
