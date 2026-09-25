package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitCreateRequest;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitResponse;
import vn.edu.fpt.cares.dto.customervisit.CustomerVisitUpdateRequest;
import vn.edu.fpt.cares.enums.VisitStatus;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.service.AuthService;
import vn.edu.fpt.cares.service.CustomerVisitService;
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
@RequestMapping("/api/v1/customer-visits")
@RequiredArgsConstructor
public class CustomerVisitController {

    private final CustomerVisitService service;
    private final AuthService authService;
    private final vn.edu.fpt.cares.service.SameDayParaclinicalResultService sameDayResultService;
    private final vn.edu.fpt.cares.service.StaffDutyService staffDutyService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<CustomerVisitResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<CustomerVisitResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @GetMapping("/customers/{customerId}/same-day-paraclinical-results")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<java.util.List<vn.edu.fpt.cares.dto.medicalhistory.SameDayParaclinicalResultResponse>>
    sameDayResultsForReception(@PathVariable UUID customerId) {
        return RestResponses.ok(sameDayResultService.findForCustomerToday(customerId));
    }

    @GetMapping("/customers/{customerId}/same-day-examination-services")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<java.util.List<vn.edu.fpt.cares.dto.customervisit.SameDayExaminationServiceResponse>>
    sameDayExaminationServices(@PathVariable UUID customerId) {
        return RestResponses.ok(service.getSameDayExaminationServices(customerId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<CustomerVisitResponse> create(@Valid @RequestBody CustomerVisitCreateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        UUID issuedById = authService.currentStaffId();
        var updatedReq = new CustomerVisitCreateRequest(
                req.customerId(),
                req.appointmentId(),
                req.serviceIds(),
                issuedById,
                req.guestFullName(),
                req.guestPhone(),
                req.guestAddress(),
                req.guestDateOfBirth(),
                req.guestGender(),
                req.guestEmail(),
                req.guestBloodType(),
                req.allergyStatus(),
                req.guestAllergies(),
                req.updatePatientProfile(),
                req.insuranceId()
        );
        CustomerVisitResponse created = service.create(updatedReq);
        return RestResponses.created("/api/v1/customer-visits/{id}", created.visitId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<CustomerVisitResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody CustomerVisitUpdateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.RECEPTIONIST);
        service.delete(id);
        return RestResponses.noContent();
    }
}



