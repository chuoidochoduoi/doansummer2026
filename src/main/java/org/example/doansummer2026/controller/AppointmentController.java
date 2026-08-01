package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInRequest;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInResponse;
import org.example.doansummer2026.dto.appointment.GuestCheckInRequest;
import org.example.doansummer2026.dto.appointment.GuestCheckInResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentGuestCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentResponse;
import org.example.doansummer2026.dto.appointment.AppointmentUpdateRequest;
import org.example.doansummer2026.dto.appointment.GuestHistoryResponse;
import org.example.doansummer2026.dto.appointment.CustomerAppointmentResponse;
import org.example.doansummer2026.dto.appointment.CustomerAppointmentDetailResponse;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.service.AppointmentService;
import org.example.doansummer2026.service.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<PageResponse<AppointmentResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = AuditAction.CREATE, entityName = "Appointment")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest req) {
        AppointmentResponse created = service.create(req);
        return RestResponses.created("/api/v1/appointments/{id}", created.appointmentId(), created);
    }

    /** Endpoint public cho phep dat lich cua khach khong dang nhap. */
    @PostMapping("/guest")
    @Auditable(action = AuditAction.CREATE, entityName = "Appointment")
    public ResponseEntity<AppointmentResponse> createForGuest(@Valid @RequestBody AppointmentGuestCreateRequest req) {
        AppointmentResponse created = service.createForGuest(req);
        return RestResponses.created("/api/v1/appointments/{id}", created.appointmentId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<AppointmentResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody AppointmentUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * Check-in tu appointment: tao ra CustomerVisit + Invoice.
     * - QueueTicket se duoc tao khi Invoice duoc thanh toan.
     * - issuedById se tu dong lay tu staff dang dang nhap neu khong truyen.
     */
    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_STAFF')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<AppointmentCheckInResponse> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentCheckInRequest req) {
        UUID issuedById = req.issuedById() != null ? req.issuedById() : authService.currentStaffId();
        return RestResponses.ok(service.checkIn(new AppointmentCheckInRequest(id, req.serviceIds(), issuedById, req.insuranceId())));
    }

    /**
     * Check-in truc tiep cho khach vang lai (khong co appointment).
     * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
     */
    @PostMapping("/guest-check-in")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Appointment")
    public ResponseEntity<GuestCheckInResponse> guestCheckIn(
            @Valid @RequestBody GuestCheckInRequest req) {
        return RestResponses.ok(service.guestCheckIn(req));
    }

    /**
     * Kiem tra guest da tung den kham chua (theo phone).
     * - Dung de hien thi thong tin guest cu khi dang ky/ check-in lan 2.
     */
    @GetMapping("/guest-history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<List<GuestHistoryResponse>> getGuestHistory(
            @RequestParam String phone) {
        List<GuestHistoryResponse> history = service.getGuestHistoryByPhone(phone);
        return RestResponses.ok(history);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<CustomerAppointmentResponse>> getMyAppointments(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return RestResponses.ok(service.getMyAppointments(authService.currentAccount().getAccountId(), code, specialty, status, pageable));
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerAppointmentDetailResponse> getMyAppointmentDetail(@PathVariable UUID id) {
        return RestResponses.ok(service.getMyAppointmentDetail(authService.currentAccount().getAccountId(), id));
    }

    @PutMapping("/my/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<CustomerAppointmentDetailResponse> updateMyAppointment(@PathVariable UUID id,
                                                                                 @Valid @RequestBody AppointmentUpdateRequest req) {
        return RestResponses.ok(service.updateMyAppointment(authService.currentAccount().getAccountId(), id, req));
    }

    @PostMapping("/my/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<Void> cancelMyAppointment(@PathVariable UUID id) {
        service.cancelMyAppointment(authService.currentAccount().getAccountId(), id);
        return RestResponses.noContent();
    }
}



