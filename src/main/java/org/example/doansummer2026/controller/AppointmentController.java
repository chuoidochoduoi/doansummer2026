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
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.service.AppointmentService;
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

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<PageResponse<AppointmentResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest req) {
        AppointmentResponse created = service.create(req);
        return RestResponses.created("/api/v1/appointments/{id}", created.appointmentId(), created);
    }

    /** Endpoint public cho phep dat lich cua khach khong dang nhap. */
    @PostMapping("/guest")
    public ResponseEntity<AppointmentResponse> createForGuest(@Valid @RequestBody AppointmentGuestCreateRequest req) {
        AppointmentResponse created = service.createForGuest(req);
        return RestResponses.created("/api/v1/appointments/{id}", created.appointmentId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<AppointmentResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody AppointmentUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    /**
     * Check-in tu appointment: tao ra CustomerVisit + Invoice.
     * - QueueTicket se duoc tao khi Invoice duoc thanh toan.
     */
    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<AppointmentCheckInResponse> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentCheckInRequest req) {
        return RestResponses.ok(service.checkIn(new AppointmentCheckInRequest(id, req.serviceIds(), req.issuedById())));
    }

    /**
     * Check-in truc tiep cho khach vang lai (khong co appointment).
     * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
     */
    @PostMapping("/guest-check-in")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<GuestCheckInResponse> guestCheckIn(
            @Valid @RequestBody GuestCheckInRequest req) {
        return RestResponses.ok(service.guestCheckIn(req));
    }
}