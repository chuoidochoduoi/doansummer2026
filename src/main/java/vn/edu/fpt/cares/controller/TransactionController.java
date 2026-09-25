package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.transaction.TransactionCreateRequest;
import vn.edu.fpt.cares.dto.transaction.TransactionResponse;
import vn.edu.fpt.cares.dto.transaction.TransactionUpdateRequest;
import vn.edu.fpt.cares.enums.TransactionStatus;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.service.TransactionService;
import vn.edu.fpt.cares.service.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;
    private final AuthService authService;
    private final vn.edu.fpt.cares.service.StaffDutyService staffDutyService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<TransactionResponse>> list(
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(invoiceId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<TransactionResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionCreateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        UUID receivedById = authService.currentStaffId();
        TransactionResponse created = service.create(new TransactionCreateRequest(
                req.invoiceId(), req.amount(), req.paymentMethod(), req.gatewayReference(),
                req.note(), receivedById));
        return RestResponses.created("/api/v1/transactions/{id}", created.transactionId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody TransactionUpdateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<TransactionResponse> confirm(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.confirm(id));
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<TransactionResponse> fail(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.fail(id));
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        service.delete(id);
        return RestResponses.noContent();
    }
}







