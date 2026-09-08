package org.example.doansummer2026.dto.journey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record QueueReturnRequestResponse(
        UUID queueTicketId,
        UUID visitId,
        String visitCode,
        String patientName,
        String patientCode,
        String phone,
        LocalDate dateOfBirth,
        Integer age,
        String roomName,
        String roomCode,
        Integer queueNumber,
        LocalDateTime calledAt,
        LocalDateTime requestedAt,
        String status,
        String message) {
}
