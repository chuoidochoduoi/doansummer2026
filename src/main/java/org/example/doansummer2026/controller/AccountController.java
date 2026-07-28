package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.account.AccountManagementResponse;
import org.example.doansummer2026.dto.account.AccountResponse;
import org.example.doansummer2026.dto.account.AccountUpdateRequest;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.service.AccountService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AccountController {

    private final AccountService accountService;
    private final StaffInfoRepository staffRepo;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_STAFF')")
    public ResponseEntity<PageResponse<AccountResponse>> list(
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        return RestResponses.ok(accountService.list(role, pageable));
    }

    /**
     * API danh sach tai khoan nhan su (staff) - CHỉ ADMIN vaf CLINIC_MANAGER.
     */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<AccountManagementResponse>> listStaff(
            @RequestParam(required = false) SystemRole systemRole,
            Pageable pageable) {
        return RestResponses.ok(accountService.listStaff(systemRole, pageable));
    }

    /**
     * API danh sach tai khoan khach hang (customer) - CHỉ ADMIN vaf CLINIC_MANAGER.
     */
    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<PageResponse<AccountManagementResponse>> listCustomers(Pageable pageable) {
        return RestResponses.ok(accountService.listCustomers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        var account = accountService.findById(id);
        SystemRole systemRole = staffRepo.findByProfile_Account_Username(account.getUsername())
                .map(staff -> staff.getSystemRole())
                .orElse(null);
        return RestResponses.ok(AccountResponse.from(account, systemRole));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id,
                                                  @RequestBody AccountUpdateRequest req) {
        var account = accountService.update(id, req);
        SystemRole systemRole = staffRepo.findByProfile_Account_Username(account.getUsername())
                .map(staff -> staff.getSystemRole())
                .orElse(null);
        return RestResponses.ok(AccountResponse.from(account, systemRole));
    }

    /**
     * Khóa/mở tài khoản - CHỉ ADMIN
     * KHÔNG cho phép khóa tài khoản ADMIN hoặc CLINIC_MANAGER.
     */
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<AccountResponse> lock(@PathVariable UUID id) {
        var account = accountService.lock(id);
        SystemRole systemRole = staffRepo.findByProfile_Account_Username(account.getUsername())
                .map(staff -> staff.getSystemRole())
                .orElse(null);
        return RestResponses.ok(AccountResponse.from(account, systemRole));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLINIC_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.softDelete(id);
        return RestResponses.noContent();
    }
}



