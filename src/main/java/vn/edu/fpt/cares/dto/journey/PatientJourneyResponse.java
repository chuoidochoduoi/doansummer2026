package vn.edu.fpt.cares.dto.journey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientJourneyResponse(
        UUID visitId, String visitCode, String patientName, String phone, boolean guest,
        String currentStep, String currentRoom, String currentStatus, String nextStep,
        UUID responsibleDoctorId, String responsibleDoctorName,
        LocalDateTime checkInTime, long waitingMinutes, boolean warning, List<Step> steps,
        String currentStepId, Integer queueNumber, Integer waitingPosition,
        String priorityCategory, String priorityLabel, LocalDateTime appointmentScheduledAt) {
    public record Step(String id, String kind, String serviceName, String roomName, String roomCode, Integer queueNumber,
                       String status, LocalDateTime startedAt, LocalDateTime completedAt,
                       List<ServiceProgress> services, int totalServices, int completedServices,
                       String phase, Integer cycleNumber, UUID queueTicketId, UUID invoiceId) {}

    public record ServiceProgress(UUID serviceId, String serviceCode, String serviceName, String status) {}
}
