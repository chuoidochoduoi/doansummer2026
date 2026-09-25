package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.common.ReceptionistRecordPageResponse;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.invoice.InvoiceCreateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceResponse;
import vn.edu.fpt.cares.dto.invoice.InvoiceUpdateRequest;
import vn.edu.fpt.cares.dto.invoice.InvoiceInsuranceRequest;
import vn.edu.fpt.cares.dto.invoice.PaymentHistoryResponse;
import vn.edu.fpt.cares.dto.invoice.ReceiptDetailResponse;
import vn.edu.fpt.cares.dto.invoice.ReceiptPrintResponse;
import vn.edu.fpt.cares.dto.payment.PayOSPaymentResponse;
import vn.edu.fpt.cares.enums.InvoiceStatus;
import vn.edu.fpt.cares.enums.PaymentMethod;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.service.AuthService;
import vn.edu.fpt.cares.service.InvoiceService;
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
import vn.edu.fpt.cares.aop.Auditable;
import vn.edu.fpt.cares.enums.AuditAction;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final AuthService authService;
    private final vn.edu.fpt.cares.service.PayOSService payOSService;
    private final vn.edu.fpt.cares.service.FamilyAccessService familyAccessService;
    private final vn.edu.fpt.cares.service.StaffDutyService staffDutyService;

    // --- MAIN ENDPOINTS ---

    @GetMapping("/api/v1/invoices")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<InvoiceResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(service.get(id));
    }

    @PostMapping("/api/v1/invoices")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.CREATE, entityName = "Invoice")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        UUID issuedById = authService.currentStaffId();
        InvoiceResponse created = service.create(new InvoiceCreateRequest(
                req.customerId(), req.visitId(), req.medicalRecordId(), req.dueDate(),
                req.discount(), req.tax(), req.note(), issuedById, req.items()));
        return RestResponses.created("/api/v1/invoices/{id}", created.invoiceId(), created);
    }

    @PutMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody InvoiceUpdateRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.update(id, req));
    }

    @PostMapping("/api/v1/invoices/{id}/insurance")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> applyInsurance(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceInsuranceRequest req) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.applyInsurance(id, req));
    }

    @PostMapping("/api/v1/invoices/{id}/issue")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.issue(id));
    }

    @PostMapping("/api/v1/invoices/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.cancel(id));
    }

    @PostMapping("/api/v1/invoices/{id}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.PAYMENT_CONFIRMED, entityName = "Invoice", idParamName = "id", description = "Xác nhận thanh toán hóa đơn")
    public ResponseEntity<InvoiceResponse> pay(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.pay(id, authService.currentStaffId()));
    }

    /**
     * PayOS payment - tạo link thanh toán.
     */
    @PostMapping("/api/v1/invoices/{id}/payos")
    @Auditable(action = AuditAction.PAYMENT_CONFIRMED, entityName = "Invoice", idParamName = "id", description = "Khởi tạo thanh toán PayOS")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<?> payosMock(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(payOSService.createPaymentLink(id));
    }

    @GetMapping("/api/v1/invoices/{id}/print")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<ReceiptPrintResponse> getPrintData(@PathVariable UUID id) {
        return RestResponses.ok(service.getReceiptPrintData(id));
    }

    @DeleteMapping("/api/v1/invoices/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.DELETE, entityName = "Invoice", idParamName = "id")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
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
            @RequestParam(required = false) UUID patientProfileId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String method,
            Pageable pageable) {
        UUID profileId = familyAccessService.resolveReadableProfile(
                authService.currentAccount().getAccountId(), patientProfileId).getProfileId();
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
            @PathVariable UUID invoiceId,
            @RequestParam(required = false) UUID patientProfileId) {
        UUID profileId = familyAccessService.resolveReadableProfile(
                authService.currentAccount().getAccountId(), patientProfileId).getProfileId();
        ReceiptDetailResponse response = service.getReceiptDetail(invoiceId, profileId);
        return RestResponses.ok(response);
    }

    private PaymentMethod parsePaymentMethod(String method) {
        return switch (method.toLowerCase()) {
            case "appbanking", "banking", "bank_transfer" -> PaymentMethod.BANK_TRANSFER;
            case "cash" -> PaymentMethod.CASH;
            case "card" -> PaymentMethod.CARD;
            case "membership_card", "membership" -> PaymentMethod.MEMBERSHIP_CARD;
            default -> null;
        };
    }
}


