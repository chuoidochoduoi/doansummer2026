package org.example.doansummer2026.dto.journey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Customer read model: never expose another patient's identifiers or clinical data. */
public record PatientQueueResponse(
        UUID visitId, String roomName, String roomCode, LocalDate workDate,
        String currentStatus, Integer waitingPosition, Integer peopleAhead,
        Integer queueNumber, String priorityCategory, String priorityLabel,
        LocalDateTime appointmentScheduledAt,
        boolean canRequestReturn, String returnRequestStatus, LocalDateTime returnRequestedAt,
        List<Entry> serving, List<Entry> waiting) {
    public record Entry(Integer position, boolean self, String status) {}
}
