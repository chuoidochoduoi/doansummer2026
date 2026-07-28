package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInRequest;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInResponse;
import org.example.doansummer2026.dto.appointment.GuestCheckInRequest;
import org.example.doansummer2026.dto.appointment.GuestCheckInResponse;
import org.example.doansummer2026.dto.appointment.AppointmentResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentGuestCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentUpdateRequest;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.model.Appointment;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Service interface for Appointment management. */
public interface AppointmentServiceInterface {
    PageResponse<AppointmentResponse> search(UUID customerId,
                                            AppointmentStatus status,
                                            LocalDateTime from, LocalDateTime to,
                                            Pageable pageable);
    AppointmentResponse get(UUID id);
    AppointmentResponse create(AppointmentCreateRequest req);
    AppointmentResponse createForGuest(AppointmentGuestCreateRequest req);
    AppointmentResponse update(UUID id, AppointmentUpdateRequest req);
    AppointmentCheckInResponse checkIn(AppointmentCheckInRequest req);
    GuestCheckInResponse guestCheckIn(GuestCheckInRequest req);
    void delete(UUID id);
    Appointment findById(UUID id);
}



