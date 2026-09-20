package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.aop.Auditable;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.membership.*;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.MembershipPolicy;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.MembershipCardService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/membership-cards")
@RequiredArgsConstructor
public class MembershipCardController {
    private final MembershipCardService service;
    private final AuthService authService;
    private final org.example.doansummer2026.service.StaffDutyService staffDutyService;

    @PostMapping("/my/register")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Auditable(action = AuditAction.CREATE, entityName = "MembershipCard", description = "Đăng ký thẻ trả trước CareS")
    public ResponseEntity<MembershipCardResponse> register(@Valid @RequestBody MembershipCardRequest request) {
        return RestResponses.ok(service.register(authService.currentAccount().getAccountId(), request));
    }

    @PostMapping("/my/reset-pin")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MembershipCard", description = "Đặt lại mã PIN thẻ trả trước CareS")
    public ResponseEntity<MembershipCardResponse> resetMyPin(@Valid @RequestBody MembershipPinResetRequest request) {
        return RestResponses.ok(service.resetPin(authService.currentAccount().getAccountId(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<MembershipCardResponse> myCard() {
        return RestResponses.ok(service.myCard(authService.currentAccount().getAccountId()));
    }

    @GetMapping("/my/history")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<?> history(Pageable pageable) {
        return RestResponses.ok(service.history(authService.currentAccount().getAccountId(), pageable));
    }

    @PostMapping("/my/pay")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Auditable(action = AuditAction.PAYMENT_CONFIRMED, entityName = "MembershipCard", description = "Thanh toán bằng thẻ trả trước CareS")
    public ResponseEntity<?> pay(@Valid @RequestBody MembershipPaymentRequest request) {
        return RestResponses.ok(service.pay(authService.currentAccount().getAccountId(), request));
    }

    @PostMapping("/{cardCode}/top-up")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.CREATE, entityName = "MembershipCard", idParamName = "cardCode", description = "Nạp tiền thẻ trả trước CareS")
    public ResponseEntity<MembershipTopUpResponse> topUp(@PathVariable String cardCode,
            @Valid @RequestBody MembershipTopUpRequest request) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.topUp(cardCode, request, authService.currentStaffId()));
    }

    @PostMapping("/pay-at-counter")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.PAYMENT_CONFIRMED, entityName = "MembershipCard", description = "Thanh toán hóa đơn bằng thẻ trả trước tại quầy")
    public ResponseEntity<?> payAtCounter(@Valid @RequestBody MembershipCounterPaymentRequest request) {
        staffDutyService.requireCurrentStaffOnDuty(SystemRole.CASHIER);
        return RestResponses.ok(service.payAtCounter(request));
    }

    @GetMapping("/policy")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER','ROLE_CASHIER','ROLE_CUSTOMER')")
    public ResponseEntity<MembershipPolicy> policy() { return RestResponses.ok(service.getPolicy()); }

    @PutMapping("/policy")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "MembershipPolicy", description = "Cập nhật chính sách thẻ trả trước CareS")
    public ResponseEntity<MembershipPolicy> updatePolicy(@Valid @RequestBody MembershipPolicyRequest request) {
        return RestResponses.ok(service.updatePolicy(request));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<?> allLedger(Pageable pageable) {
        return RestResponses.ok(service.allHistory(pageable));
    }

    @GetMapping("/top-ups")
    @PreAuthorize("hasAnyAuthority('ROLE_CASHIER','ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    public ResponseEntity<?> topUpHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RestResponses.ok(service.topUpHistory(org.springframework.data.domain.PageRequest.of(
                Math.max(0, page), Math.max(1, Math.min(size, 100)))));
    }

    @PostMapping("/ledger/{ledgerId}/reverse")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "MembershipCard", idParamName = "ledgerId", description = "Hoàn tác thanh toán thẻ trả trước CareS")
    public ResponseEntity<MembershipLedgerResponse> reverse(@PathVariable java.util.UUID ledgerId,
            @Valid @RequestBody MembershipReversalRequest request) {
        return RestResponses.ok(service.reverse(ledgerId, request, authService.currentStaffId()));
    }
}
