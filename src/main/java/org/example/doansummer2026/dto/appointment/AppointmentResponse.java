package org.example.doansummer2026.dto.appointment;

import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

public record AppointmentResponse(
        UUID appointmentId,
        UUID customerId,
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String cancelReason,
        LocalDateTime createdAt,
        Boolean isGuest,
        String guestFullName,
        String guestPhone,
        String guestEmail,
        String guestAddress,
        Integer guestAge,
        LocalDate guestDateOfBirth,
        Gender guestGender,
        List<ServiceInfo> services,
        String shiftName,
        String shiftTime
) {
    public static AppointmentResponse from(Appointment a) {
        // Copy thong tin tu profile sang guest fields khi co customer
        String guestFullName = a.getGuestFullName();
        String guestPhone = a.getGuestPhone();
        String guestEmail = a.getGuestEmail();
        String guestAddress = a.getGuestAddress();
        Integer guestAge = a.getGuestAge();
        Gender guestGender = a.getGuestGender();

        // Khi co customer (khach dang nhap), copy thong tin profile sang guest fields
        if (a.getCustomer() != null) {
            guestFullName = a.getCustomer().getFullName();
            guestPhone = a.getCustomer().getPhone();
            guestEmail = a.getCustomer().getEmail();
            guestAddress = a.getCustomer().getAddress();
            // Tinh tuoi tu ngay sinh
            if (a.getCustomer().getDateOfBirth() != null) {
                guestAge = Period.between(a.getCustomer().getDateOfBirth(), LocalDate.now()).getYears();
            }
            guestGender = a.getCustomer().getGender();
        }

        List<ServiceInfo> serviceInfos = a.getServices() != null
                ? a.getServices().stream()
                        .map(s -> new ServiceInfo(s.getServiceId(), s.getName(), s.getPrice()))
                        .toList()
                : List.of();

        return new AppointmentResponse(
                a.getAppointmentId(),
                a.getCustomer() != null ? a.getCustomer().getProfileId() : null,
                a.getScheduledAt(), a.getStatus(), a.getCancelReason(), a.getCreatedAt(),
                a.getIsGuest(), guestFullName, guestPhone, guestEmail, guestAddress,
                guestAge, a.getCustomer() != null ? a.getCustomer().getDateOfBirth() : null,
                guestGender, serviceInfos, a.getShiftName(), a.getShiftTime()
        );
    }
}



