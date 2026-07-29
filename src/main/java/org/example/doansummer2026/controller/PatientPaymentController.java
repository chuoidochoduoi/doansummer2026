package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.ReceptionistRecordPageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.invoice.PaymentHistoryResponse;
import org.example.doansummer2026.dto.invoice.ReceiptDetailResponse;
import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.service.InvoiceService;
import org.example.doansummer2026.service.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient/payments")
@RequiredArgsConstructor
public class PatientPaymentController {

    private final InvoiceService invoiceService;
    private final AuthService authService;

    /**
     * API lich su thanh toan cua benh nhan.
     * GET /api/patient/payments
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<ReceptionistRecordPageResponse<PaymentHistoryResponse>> getPaymentHistory(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String method,
            Pageable pageable) {
        UUID profileId = authService.currentProfileId();
        if (profileId == null) {
            var emptyPage = org.springframework.data.domain.Page.<PaymentHistoryResponse>empty(pageable);
            return RestResponses.ok(new ReceptionistRecordPageResponse<>(java.util.Collections.emptyList(), 0L, 0));
        }
        LocalDate from = (fromDate != null && !fromDate.isBlank()) ? LocalDate.parse(fromDate) : null;
        LocalDate to = (toDate != null && !toDate.isBlank()) ? LocalDate.parse(toDate) : null;
        PaymentMethod paymentMethod = (method != null && !method.isBlank()) ? parsePaymentMethod(method) : null;

        var pageResponse = invoiceService.getPaymentHistoryForPatient(
                profileId, from, to, paymentMethod, pageable
        );

        return RestResponses.ok(ReceptionistRecordPageResponse.from(pageResponse));
    }

    /**
     * API chi tiet phieu thu.
     * GET /api/patient/payments/{invoiceId}
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ADMIN')")
    public ResponseEntity<ReceiptDetailResponse> getReceiptDetail(
            @PathVariable UUID invoiceId) {
        ReceiptDetailResponse response = invoiceService.getReceiptDetail(invoiceId, authService.currentProfileId());
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