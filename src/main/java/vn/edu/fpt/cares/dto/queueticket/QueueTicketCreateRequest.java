package vn.edu.fpt.cares.dto.queueticket;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record QueueTicketCreateRequest(
        @NotNull UUID visitId,
        @NotNull UUID departmentId,
        @NotNull UUID serviceId,
        LocalDate workDate
) {}




