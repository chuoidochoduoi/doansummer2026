package vn.edu.fpt.cares.dto.schedule;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.cares.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleCreateRequest(
        @NotNull UUID staffId,
        @NotNull LocalDate workDate,
        @NotNull UUID shiftId,
        ScheduleStatus status,
        Boolean isCustom,
        UUID templateId,
        String note
) {}



