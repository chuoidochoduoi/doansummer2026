package vn.edu.fpt.cares.dto.queueticket;

import vn.edu.fpt.cares.enums.QueueStatus;

import java.util.List;
import java.util.UUID;

/**
 * Nhom cac dich vu kham duoc thuc hien lien tiep trong cung mot phong.
 * Moi muc van giu QueueTicket va MedicalRecord rieng; DTO chi gom cach hien thi
 * va dieu phoi mot lan goi benh nhan.
 */
public record SameRoomExaminationChainResponse(
        UUID visitId,
        UUID departmentId,
        String departmentName,
        Integer displayQueueNumber,
        UUID currentTicketId,
        int currentPosition,
        int totalServices,
        int completedServices,
        List<ServiceStep> services
) {
    public record ServiceStep(
            UUID ticketId,
            UUID recordId,
            UUID serviceId,
            String serviceCode,
            String serviceName,
            QueueStatus status
    ) {}
}
