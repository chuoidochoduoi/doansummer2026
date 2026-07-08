package org.example.doansummer2026.dto.queueTicket;

import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.QueueTicket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record QueueTicketResponse(
        UUID ticketId,
        UUID visitId,
        UUID departmentId,
        String departmentName,
        UUID serviceId,
        String serviceName,
        BigDecimal servicePrice,
        LocalDate workDate,
        Integer queueNumber,
        QueueStatus status,
        LocalDateTime calledAt,
        LocalDateTime completedAt
) {
    public static QueueTicketResponse from(QueueTicket q) {
        UUID visitId = q.getVisit() != null ? q.getVisit().getVisitId() : null;
        UUID deptId = q.getDepartment() != null ? q.getDepartment().getDepartmentId() : null;
        String deptName = q.getDepartment() != null ? q.getDepartment().getName() : null;
        UUID serviceId = q.getService() != null ? q.getService().getServiceId() : null;
        String serviceName = q.getService() != null ? q.getService().getName() : null;
        BigDecimal servicePrice = q.getService() != null ? q.getService().getPrice() : null;
        return new QueueTicketResponse(q.getTicketId(), visitId, deptId, deptName,
                serviceId, serviceName, servicePrice,
                q.getWorkDate(), q.getQueueNumber(), q.getStatus(), q.getCalledAt(), q.getCompletedAt());
    }
}
