package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.appointment.AppointmentCheckInRequest;
import vn.edu.fpt.cares.dto.appointment.AppointmentCheckInResponse;
import vn.edu.fpt.cares.dto.appointment.GuestCheckInRequest;
import vn.edu.fpt.cares.dto.appointment.GuestCheckInResponse;
import vn.edu.fpt.cares.dto.appointment.AppointmentResponse;
import vn.edu.fpt.cares.dto.appointment.AppointmentCreateRequest;
import vn.edu.fpt.cares.dto.appointment.AppointmentGuestCreateRequest;
import vn.edu.fpt.cares.dto.appointment.AppointmentUpdateRequest;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentResponse;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentDetailResponse;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentCreateRequest;
import vn.edu.fpt.cares.dto.appointment.GroupAppointmentCreateRequest;
import vn.edu.fpt.cares.enums.AppointmentStatus;
import vn.edu.fpt.cares.model.Appointment;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

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
    AppointmentResponse createMy(UUID accountId, CustomerAppointmentCreateRequest req);
    List<AppointmentResponse> createMyGroup(UUID accountId, GroupAppointmentCreateRequest req);

    PageResponse<CustomerAppointmentResponse> getMyAppointments(UUID accountId, UUID patientProfileId,
                                                                 boolean includeFamily, String code,
                                                                 String specialty, String status,
                                                                 LocalDateTime from, LocalDateTime to,
                                                                 Pageable pageable);
    PageResponse<CustomerAppointmentResponse> getMyAppointments(UUID accountId, String code,
                                                                 String specialty, String status,
                                                                 LocalDateTime from, LocalDateTime to,
                                                                 Pageable pageable);
    CustomerAppointmentDetailResponse getMyAppointmentDetail(UUID customerId, UUID appointmentId);
    CustomerAppointmentDetailResponse updateMyAppointment(UUID customerId, UUID appointmentId, AppointmentUpdateRequest req);
    void cancelMyAppointment(UUID customerId, UUID appointmentId);
}



