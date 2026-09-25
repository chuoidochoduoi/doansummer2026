package vn.edu.fpt.cares.dto.shift;

import vn.edu.fpt.cares.enums.ClinicScheduleExceptionType;
import vn.edu.fpt.cares.model.ClinicScheduleException;
import java.time.*;
import java.util.UUID;

public record ClinicScheduleExceptionResponse(
        UUID exceptionId, LocalDate workDate, UUID shiftId, String shiftName,
        ClinicScheduleExceptionType type, LocalTime specialStartTime, LocalTime specialEndTime,
        String reason, LocalDateTime createdAt
) {
    public static ClinicScheduleExceptionResponse from(ClinicScheduleException value) {
        return new ClinicScheduleExceptionResponse(value.getExceptionId(), value.getWorkDate(),
                value.getShift() == null ? null : value.getShift().getShiftId(),
                value.getShift() == null ? null : value.getShift().getName(), value.getType(),
                value.getSpecialStartTime(), value.getSpecialEndTime(), value.getReason(),
                value.getCreatedAt());
    }
}
