package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.account.AccountResponse;
import org.example.doansummer2026.dto.account.AccountUpdateRequest;
import org.example.doansummer2026.enums.Role;
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
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<PageResponse<AccountResponse>> list(
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        return RestResponses.ok(accountService.list(role, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(AccountResponse.from(accountService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id,
                                                  @RequestBody AccountUpdateRequest req) {
        return RestResponses.ok(AccountResponse.from(accountService.update(id, req)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> setStatus(@PathVariable UUID id,
                                          @RequestParam Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            accountService.update(id, new AccountUpdateRequest(null, null, true));
        } else {
            accountService.softDelete(id);
        }
        return RestResponses.noContent();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.softDelete(id);
        return RestResponses.noContent();
    }
}