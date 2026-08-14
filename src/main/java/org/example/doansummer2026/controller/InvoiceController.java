package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceInsuranceRequest;
import org.example.doansummer2026.dto.invoice.PaymentHistoryResponse;
import org.example.doansummer2026.dto.invoice.ReceiptDetailResponse;
import org.example.doansummer2026.dto.invoice.ReceiptPrintResponse;
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
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.enums.AuditAction;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final AuthService authService;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/invoices")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<InvoiceResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return RestResponses.ok(service.search(customerId, status, search, category, from, to, pageable));
    }

    @GetMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping("/api/v1/invoices")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.CREATE, entityName = "Invoice")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest req) {
        UUID issuedById = authService.currentStaffId();
        InvoiceResponse created = service.create(new InvoiceCreateRequest(
                req.customerId(), req.visitId(), req.medicalRecordId(), req.dueDate(),
                req.discount(), req.tax(), req.note(), issuedById, req.items()));
        return RestResponses.created("/api/v1/invoices/{id}", created.invoiceId(), created);
    }

    @PutMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody InvoiceUpdateRequest req) {
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/api/v1/invoices/{id}/insurance")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> applyInsurance(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceInsuranceRequest req) {
        return RestResponses.ok(service.applyInsurance(id, req));
    }

    @PostMapping("/api/v1/invoices/{id}/issue")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
        return RestResponses.ok(service.issue(id));
    }

    @PostMapping("/api/v1/invoices/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        return RestResponses.ok(service.cancel(id));
    }

    @PostMapping("/api/v1/invoices/{id}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.PAYMENT_CONFIRMED, entityName = "Invoice", idParamName = "id", description = "Xác nhận thanh toán hóa đơn")
    public ResponseEntity<InvoiceResponse> pay(@PathVariable UUID id) {
        return RestResponses.ok(service.pay(id, authService.currentStaffId()));
    }

    /**
     * Mock PayOS payment - tạo link thanh toán giả lập.
     * Trong môi trường thực tế sẽ gọi API PayOS SDK.
     */
    @PostMapping("/api/v1/invoices/{id}/payos")
    @Auditable(action = AuditAction.PAYMENT_CONFIRMED, entityName = "Invoice", idParamName = "id", description = "Khởi tạo thanh toán PayOS")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    public ResponseEntity<ReceiptPrintResponse> getPrintData(@PathVariable UUID id) {
        return RestResponses.ok(service.getReceiptPrintData(id));
    }

    @DeleteMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN')")
    @Auditable(action = AuditAction.DELETE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return RestResponses.noContent();
    }

    // --- PATIENT ENDPOINTS ---

    /**
     * API lich su thanh toan cua benh nhan.
     */
    @GetMapping("/api/patient/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_ADMIN')")
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


