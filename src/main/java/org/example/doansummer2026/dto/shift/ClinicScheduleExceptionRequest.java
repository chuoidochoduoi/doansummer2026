package org.example.doansummer2026.dto.shift;

import jakarta.validation.constraints.*;
import org.example.doansummer2026.enums.ClinicScheduleExceptionType;
import java.time.*;
import java.util.UUID;

public record ClinicScheduleExceptionRequest(
        @NotNull LocalDate workDate,
        UUID shiftId,
        @NotNull ClinicScheduleExceptionType type,
        LocalTime specialStartTime,
        LocalTime specialEndTime,
        @NotBlank @Size(max = 500) String reason
) {}
