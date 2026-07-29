package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
import org.example.doansummer2026.dto.invoice.PaymentHistoryResponse;
import org.example.doansummer2026.dto.invoice.ReceiptDetailResponse;
import org.example.doansummer2026.dto.payment.PayOSPaymentResponse;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.service.AuthService;
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
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final AuthService authService;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/invoices")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<PageResponse<InvoiceResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, from, to, pageable));
    }

    @GetMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping("/api/v1/invoices")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest req) {
        InvoiceResponse created = service.create(req);
        return RestResponses.created("/api/v1/invoices/{id}", created.invoiceId(), created);
    }

    @PutMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody InvoiceUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/api/v1/invoices/{id}/issue")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
        return RestResponses.ok(service.issue(id));
    }

    @PostMapping("/api/v1/invoices/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        return RestResponses.ok(service.cancel(id));
    }

    @PostMapping("/api/v1/invoices/{id}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> pay(@PathVariable UUID id) {
        return RestResponses.ok(service.pay(id));
    }

    /**
     * Mock PayOS payment - tạo link thanh toán giả lập.
     * Trong môi trường thực tế sẽ gọi API PayOS SDK.
     */
    @PostMapping("/api/v1/invoices/{id}/payos")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<?> payosMock(@PathVariable UUID id) {
        InvoiceResponse invoice = service.get(id);
        if (invoice.status() == InvoiceStatus.PAID) {
            return RestResponses.ok(PayOSPaymentResponse.paid(
                    invoice.invoiceId(), invoice.invoiceCode(), invoice.totalAmount()
            ));
        }
        return RestResponses.ok(PayOSPaymentResponse.pending(
                    invoice.invoiceId(), invoice.invoiceCode(), invoice.totalAmount()
        ));
    }

    @GetMapping("/api/v1/invoices/{id}/print")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<InvoiceResponse> getPrintData(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @DeleteMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    // --- PATIENT ENDPOINTS ---

    /**
     * API lich su thanh toan cua benh nhan.
     */
    @GetMapping("/api/patient/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<ReceptionistRecordPageResponse<PaymentHistoryResponse>> getPaymentHistory(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String method,
            Pageable pageable) {
        UUID profileId = authService.currentProfileId();
        if (profileId == null) {
            return RestResponses.ok(new ReceptionistRecordPageResponse<>(java.util.Collections.emptyList(), 0L, 0));
        }
        LocalDate from = (fromDate != null && !fromDate.isBlank()) ? LocalDate.parse(fromDate) : null;
        LocalDate to = (toDate != null && !toDate.isBlank()) ? LocalDate.parse(toDate) : null;
        PaymentMethod paymentMethod = (method != null && !method.isBlank()) ? parsePaymentMethod(method) : null;

        var pageResponse = service.getPaymentHistoryForPatient(
                profileId, from, to, paymentMethod, pageable
        );

        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    /**
     * API chi tiet phieu thu.
     */
    @GetMapping("/api/patient/payments/{invoiceId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<ReceiptDetailResponse> getReceiptDetail(
            @PathVariable UUID invoiceId) {
        ReceiptDetailResponse response = service.getReceiptDetail(invoiceId, authService.currentProfileId());
        return RestResponses.ok(response);
    }

    private PaymentMethod parsePaymentMethod(String method) {
        return switch (method.toLowerCase()) {
            case "appbanking", "banking", "bank_transfer" -> PaymentMethod.BANK_TRANSFER;
            case "cash" -> PaymentMethod.CASH;
            case "card" -> PaymentMethod.CARD;
            default -> null;
        };
    }
}




