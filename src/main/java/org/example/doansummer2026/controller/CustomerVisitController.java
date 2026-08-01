package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitUpdateRequest;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.CustomerVisitService;
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

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<PageResponse<CustomerVisitResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<CustomerVisitResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<CustomerVisitResponse> create(@Valid @RequestBody CustomerVisitCreateRequest req) {
        UUID issuedById = req.issuedById() != null ? req.issuedById() : authService.currentStaffId();
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
                req.insuranceId()
        );
        CustomerVisitResponse created = service.create(updatedReq);
        return RestResponses.created("/api/v1/customer-visits/{id}", created.visitId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<CustomerVisitResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody CustomerVisitUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RECEPTIONIST','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}



