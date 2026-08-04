package org.example.doansummer2026.dto.journey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientJourneyResponse(
        UUID visitId, String visitCode, String patientName, String phone, boolean guest,
        String currentStep, String currentRoom, String currentStatus, String nextStep,
        LocalDateTime checkInTime, long waitingMinutes, boolean warning, List<Step> steps) {
    public record Step(String id, String kind, String serviceName, String roomName, String roomCode, Integer queueNumber,
                       String status, LocalDateTime startedAt, LocalDateTime completedAt) {}
}
