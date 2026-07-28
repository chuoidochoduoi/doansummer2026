package org.example.doansummer2026.dto.queueTicket;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record QueueTicketCreateRequest(
        @NotNull UUID visitId,
        @NotNull UUID departmentId,
        @NotNull UUID serviceId,
        LocalDate workDate
) {}




