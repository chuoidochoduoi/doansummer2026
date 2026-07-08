package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.service.InvoiceService;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping
    public ResponseEntity<PageResponse<InvoiceResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest req) {
        InvoiceResponse created = service.create(req);
        return RestResponses.created("/api/v1/invoices/{id}", created.invoiceId(), created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody InvoiceUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
        return RestResponses.ok(service.issue(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        return RestResponses.ok(service.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }
}
