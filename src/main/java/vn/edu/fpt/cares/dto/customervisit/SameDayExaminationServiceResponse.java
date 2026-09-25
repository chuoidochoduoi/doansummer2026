package vn.edu.fpt.cares.dto.customervisit;

import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.VisitStatus;

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
