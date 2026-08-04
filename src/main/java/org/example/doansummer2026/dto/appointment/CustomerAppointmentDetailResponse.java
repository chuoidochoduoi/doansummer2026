package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.QueueTicket;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public record CustomerAppointmentDetailResponse(
        UUID id,
        String code,
        String status,
        String date,
        String timeSlot,
        String queueNumber,
        String reason,
        String symptoms,
        List<ServiceCostInfo> services
) {
    public record ServiceCostInfo(UUID id, String name, java.math.BigDecimal cost) {}

    public static CustomerAppointmentDetailResponse from(Appointment a) {
        String code = "APPT-" + a.getAppointmentId().toString().substring(0, 8).toUpperCase();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String date = a.getScheduledAt() != null ? a.getScheduledAt().format(dateFormatter) : "";
        
        String timeSlotStr = "";
        if (a.getTimeSlot() != null) {
            switch (a.getTimeSlot()) {
                case MORNING:
                    timeSlotStr = "08:00 - 12:00 (Sáng)";
                    break;
                case AFTERNOON:
                    timeSlotStr = "13:00 - 17:00 (Chiều)";
                    break;
            }
        }
        
        String queueNum = null;
        if (a.getVisit() != null && a.getVisit().getQueueTickets() != null && !a.getVisit().getQueueTickets().isEmpty()) {
            QueueTicket firstTicket = a.getVisit().getQueueTickets().iterator().next();
            queueNum = String.valueOf(firstTicket.getQueueNumber());
        }
        
        String statusStr = "upcoming";
        if (a.getStatus() != null) {
            if (a.getStatus() == org.example.doansummer2026.enums.AppointmentStatus.CANCELLED) {
                statusStr = "cancelled";
            } else if (a.getVisit() != null && a.getVisit().getStatus() == org.example.doansummer2026.enums.VisitStatus.COMPLETED) {
                statusStr = "completed";
            } else if (a.getStatus() == org.example.doansummer2026.enums.AppointmentStatus.CHECKED_IN) {
                statusStr = "checked_in";
            } else {
                statusStr = "upcoming";
            }
        }

        List<ServiceCostInfo> serviceCosts = a.getServices() != null
                ? a.getServices().stream().map(s -> new ServiceCostInfo(s.getServiceId(), s.getName(), s.getPrice())).toList()
                : List.of();

        return new CustomerAppointmentDetailResponse(
                a.getAppointmentId(),
                code,
                statusStr,
                date,
                timeSlotStr,
                queueNum,
                null,
                null,
                serviceCosts
        );
    }
}
