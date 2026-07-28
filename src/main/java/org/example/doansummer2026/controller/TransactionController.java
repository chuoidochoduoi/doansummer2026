package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.transaction.TransactionCreateRequest;
import org.example.doansummer2026.dto.transaction.TransactionResponse;
import org.example.doansummer2026.dto.transaction.TransactionUpdateRequest;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.service.TransactionService;
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

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<PageResponse<TransactionResponse>> list(
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return RestResponses.ok(service.search(invoiceId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<TransactionResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionCreateRequest req) {
        TransactionResponse created = service.create(req);
        return RestResponses.created("/api/v1/transactions/{id}", created.transactionId(), created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody TransactionUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<TransactionResponse> confirm(@PathVariable UUID id) {
        return RestResponses.ok(service.confirm(id));
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<TransactionResponse> fail(@PathVariable UUID id) {
        return RestResponses.ok(service.fail(id));
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}




