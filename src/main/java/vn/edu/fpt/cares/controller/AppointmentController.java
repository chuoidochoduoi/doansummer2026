package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.appointment.AppointmentCheckInRequest;
import vn.edu.fpt.cares.dto.appointment.AppointmentCheckInResponse;
import vn.edu.fpt.cares.dto.appointment.GuestCheckInRequest;
import vn.edu.fpt.cares.dto.appointment.GuestCheckInResponse;
import vn.edu.fpt.cares.dto.appointment.AppointmentCreateRequest;
import vn.edu.fpt.cares.dto.appointment.AppointmentGuestCreateRequest;
import vn.edu.fpt.cares.dto.appointment.AppointmentResponse;
import vn.edu.fpt.cares.dto.appointment.AppointmentUpdateRequest;
import vn.edu.fpt.cares.dto.appointment.GuestHistoryResponse;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentResponse;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentDetailResponse;
import vn.edu.fpt.cares.dto.appointment.CustomerAppointmentCreateRequest;
import vn.edu.fpt.cares.dto.appointment.GroupAppointmentCreateRequest;
import vn.edu.fpt.cares.enums.AppointmentStatus;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.service.AppointmentService;
import vn.edu.fpt.cares.service.AuthService;
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
import vn.edu.fpt.cares.aop.Auditable;
import vn.edu.fpt.cares.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final AuthService authService;
    private final vn.edu.fpt.cares.service.StaffDutyService staffDutyService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTIONIST','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<AppointmentResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_RECEPTIONIST', 'ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.CREATE, entityName = "Appointment")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        var current = authService.currentAccount();
        if (current.getRole() == vn.edu.fpt.cares.enums.Role.CUSTOMER
                && !current.getAccountId().equals(req.customerId())) {
            throw new vn.edu.fpt.cares.exception.BadRequestException(
                    "Khách hàng chỉ có thể đặt lịch cho chính mình");
        }
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<AppointmentResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody AppointmentUpdateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Appointment", idParamName = "id")
    public ResponseEntity<AppointmentCheckInResponse> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentCheckInRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        UUID issuedById = authService.currentStaffId();
        return RestResponses.ok(service.checkIn(new AppointmentCheckInRequest(
                id, req.serviceIds(), issuedById,
                req.patientFullName(), req.patientPhone(), req.patientEmail(), req.patientAddress(),
                req.patientDateOfBirth(), req.patientAge(), req.patientGender()
        )));
    }

    /**
     * Check-in truc tiep cho khach vang lai (khong co appointment).
     * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
     */
    @PostMapping("/guest-check-in")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Appointment")
    public ResponseEntity<GuestCheckInResponse> guestCheckIn(
            @Valid @RequestBody GuestCheckInRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        UUID staffId = authService.currentStaffId();
        return RestResponses.ok(service.guestCheckIn(new GuestCheckInRequest(req.guestFullName(), req.guestPhone(),
                req.guestAddress(), req.guestAge(), req.guestGender(), req.serviceIds(), staffId)));
    }

    /**
     * Kiem tra guest da tung den kham chua (theo phone).
     * - Dung de hien thi thong tin guest cu khi dang ky/ check-in lan 2.
     */
    @GetMapping("/guest-history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<List<GuestHistoryResponse>> getGuestHistory(
            @RequestParam String phone) {
        List<GuestHistoryResponse> history = service.getGuestHistoryByPhone(phone);
        return RestResponses.ok(history);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<CustomerAppointmentResponse>> getMyAppointments(
            @RequestParam(required = false) UUID patientProfileId,
            @RequestParam(defaultValue = "false") boolean includeFamily,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.getMyAppointments(authService.currentAccount().getAccountId(),
                patientProfileId, includeFamily, code, specialty, status, from, to, pageable));
    }

    @PostMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Auditable(action = AuditAction.CREATE, entityName = "Appointment")
    public ResponseEntity<AppointmentResponse> createMy(
            @Valid @RequestBody CustomerAppointmentCreateRequest req) {
        AppointmentResponse created = service.createMy(authService.currentAccount().getAccountId(), req);
        return RestResponses.created("/api/v1/appointments/my/{id}", created.appointmentId(), created);
    }

    @PostMapping("/my/group")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Auditable(action = AuditAction.CREATE, entityName = "Appointment")
    public ResponseEntity<List<AppointmentResponse>> createMyGroup(
            @Valid @RequestBody GroupAppointmentCreateRequest req) {
        return RestResponses.ok(service.createMyGroup(authService.currentAccount().getAccountId(), req));
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
