package vn.edu.fpt.cares.dto.shift;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record ShiftVersionCreateRequest(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull LocalDate effectiveFrom,
        @NotBlank @Size(max = 500) String changeReason
) {}
