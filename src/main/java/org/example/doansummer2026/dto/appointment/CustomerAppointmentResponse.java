package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.QueueTicket;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record CustomerAppointmentResponse(
        UUID id,
        String code,
        String date,
        String timeWindow,
        String shift,
        String specialty,
        String queueNumber,
        String status,
        String serviceSummary
) {
    public static CustomerAppointmentResponse from(Appointment a) {
        String code = "APPT-" + a.getAppointmentId().toString().substring(0, 8).toUpperCase();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String date = a.getScheduledAt() != null ? a.getScheduledAt().format(dateFormatter) : "";
        
        String timeWindow = a.getShiftTime() != null ? a.getShiftTime() : "";
        String shift = a.getShiftName() != null ? a.getShiftName() : "";
        
        String specialty = "Khám Bệnh Chung";
        String serviceSummary = "Chưa chọn dịch vụ";
        if (a.getServices() != null && !a.getServices().isEmpty()) {
            var firstService = a.getServices().iterator().next();
            if (firstService.getDepartment() != null) {
                specialty = firstService.getDepartment().getName();
            }
            java.util.List<String> serviceNames = a.getServices().stream()
                    .map(service -> service.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .sorted()
                    .toList();
            if (!serviceNames.isEmpty()) {
                serviceSummary = serviceNames.size() == 1
                        ? serviceNames.get(0)
                        : serviceNames.get(0) + " + " + (serviceNames.size() - 1) + " dịch vụ khác";
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

        return new CustomerAppointmentResponse(
                a.getAppointmentId(),
                code,
                date,
                timeWindow,
                shift,
                specialty,
                queueNum,
                statusStr,
                serviceSummary
        );
    }
}
