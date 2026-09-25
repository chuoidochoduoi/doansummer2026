package vn.edu.fpt.cares.dto.appointment;

import vn.edu.fpt.cares.model.Appointment;
import vn.edu.fpt.cares.model.QueueTicket;

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
        List<ServiceCostInfo> services,
        UUID patientProfileId,
        String patientCode,
        String patientName,
        boolean isSelf,
        String relationship
) {
    public record ServiceCostInfo(UUID id, String name, java.math.BigDecimal cost) {}

    public static CustomerAppointmentDetailResponse from(Appointment a) {
        return from(a, a.getCustomer() == null ? null : a.getCustomer().getProfileId(), null);
    }

    public static CustomerAppointmentDetailResponse from(Appointment a, UUID ownerProfileId, String relationship) {
        String code = "APPT-" + a.getAppointmentId().toString().substring(0, 8).toUpperCase();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String date = a.getScheduledAt() != null ? a.getScheduledAt().format(dateFormatter) : "";
        
        String timeSlotStr = "";
        if (a.getShiftTime() != null && a.getShiftName() != null) {
            timeSlotStr = a.getShiftTime() + " (" + a.getShiftName() + ")";
        } else if (a.getShiftTime() != null) {
            timeSlotStr = a.getShiftTime();
        }
        
        String queueNum = null;
        if (a.getVisit() != null && a.getVisit().getQueueTickets() != null && !a.getVisit().getQueueTickets().isEmpty()) {
            QueueTicket firstTicket = a.getVisit().getQueueTickets().iterator().next();
            queueNum = String.valueOf(firstTicket.getQueueNumber());
        }
        
        String statusStr = "upcoming";
        if (a.getStatus() != null) {
            if (a.getStatus() == vn.edu.fpt.cares.enums.AppointmentStatus.CANCELLED) {
                statusStr = "cancelled";
            } else if (a.getVisit() != null && a.getVisit().getStatus() == vn.edu.fpt.cares.enums.VisitStatus.COMPLETED) {
                statusStr = "completed";
            } else if (a.getStatus() == vn.edu.fpt.cares.enums.AppointmentStatus.CHECKED_IN) {
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
                a.getCancelReason(),
                null,
                serviceCosts,
                a.getCustomer() == null ? null : a.getCustomer().getProfileId(),
                a.getCustomer() == null ? null : a.getCustomer().getPatientCode(),
                a.getCustomer() == null ? a.getGuestFullName() : a.getCustomer().getFullName(),
                a.getCustomer() != null && a.getCustomer().getProfileId().equals(ownerProfileId),
                relationship
        );
    }
}
