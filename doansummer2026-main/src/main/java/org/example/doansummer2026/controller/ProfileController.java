package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.profile.*;
import org.example.doansummer2026.dto.profile.ProfileCustomerResponse.AppointmentSummary;
import org.example.doansummer2026.dto.profile.ProfileCustomerResponse.TestResultSummary;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.service.*;
import org.springframework.data.domain.Pageable;
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
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.ProfileService;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AppointmentService appointmentService;
    private final TestRequestService testRequestService;
    private final AuthService authService;

    /** Lay profile cua tai khoan dang nhap (cho CUSTOMER). */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileCustomerResponse> me() {
        Account me = authService.currentAccount();
        ProfileCustomerResponse response = profileService.getMyProfile(me.getAccountId());
        return RestResponses.ok(response);
    }

    /** Update profile cua tai khoan dang nhap (benh nhan tu sua). */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> updateMe(@Valid @RequestBody ProfileUpdateRequest req) {
        Account me = authService.currentAccount();
        ProfileResponse current = profileService.getByAccount(me.getAccountId());
        return RestResponses.ok(profileService.updateSelf(current.profileId(), req));
    }

    /** ADMIN xem chi tiet profile bat ky. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<ProfileResponse> get(@PathVariable UUID id) {
        return RestResponses.ok(profileService.get(id));
    }

    /** ADMIN tim kiem. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR','ROLE_NURSE','ROLE_ADMIN')")
    public ResponseEntity<PageResponse<ProfileResponse>> search(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return RestResponses.ok(profileService.search(keyword, pageable));
    }

    /** ADMIN tao profile (thuong di kem StaffService.create). */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody ProfileCreateRequest req) {
        if (req.accountId() == null) {
            throw new BadRequestException("Mã tài khoản là bắt buộc");
        }
        ProfileResponse created = profileService.create(req);
        return RestResponses.created("/api/v1/profiles/{id}", created.profileId(), created);
    }

    /** ADMIN cap nhat profile bat ky. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProfileResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody ProfileUpdateRequest req) {
        return RestResponses.ok(profileService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        profileService.delete(id);
        return RestResponses.noContent();
    }
}




