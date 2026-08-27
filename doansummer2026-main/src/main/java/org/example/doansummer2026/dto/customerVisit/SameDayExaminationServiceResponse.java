package org.example.doansummer2026.dto.customerVisit;

import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SameDayExaminationServiceResponse(
        UUID serviceId,
        String serviceCode,
        String serviceName,
        UUID visitId,
        String visitCode,
        VisitStatus visitStatus,
        QueueStatus queueStatus,
        LocalDateTime registeredAt,
        boolean locked,
        String reason
) {
}
