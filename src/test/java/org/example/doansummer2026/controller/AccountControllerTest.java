package org.example.doansummer2026.controller;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.account.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock AccountService accountService;
    @Mock StaffInfoRepository staffRepo;
    @Mock AuthService authService;
    @InjectMocks AccountController controller;

    @Test
    void listEndpointsDelegateFilters() {
        var pageable = PageRequest.of(1, 10);
        var accounts = new PageResponse<AccountResponse>(List.of(), 1, 10, 0, 0, false, true);
        var managed = new PageResponse<AccountManagementResponse>(List.of(), 1, 10, 0, 0, false, true);
        when(accountService.list(Role.STAFF, pageable)).thenReturn(accounts);
        when(accountService.listStaff("bác sĩ", SystemRole.DOCTOR, pageable)).thenReturn(managed);
        when(accountService.listCustomers("anh", "ACTIVE", pageable)).thenReturn(managed);
        assertSame(accounts, controller.list(Role.STAFF, pageable).getBody());
        assertSame(managed, controller.listStaff("bác sĩ", SystemRole.DOCTOR, pageable).getBody());
        assertSame(managed, controller.listCustomers("anh", "ACTIVE", pageable).getBody());
    }

    @Test
    void getAllowsOrdinaryStaffAndAccountsWithoutStaffProfile() {
        Account doctor = account("doctor");
        when(accountService.findById(doctor.getAccountId())).thenReturn(doctor);
        when(staffRepo.findFirstByProfile_Account_Username("doctor"))
                .thenReturn(Optional.of(StaffInfo.builder().systemRole(SystemRole.DOCTOR).build()));
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        assertEquals(SystemRole.DOCTOR, controller.get(doctor.getAccountId()).getBody().systemRole());

        Account customer = account("customer");
        when(accountService.findById(customer.getAccountId())).thenReturn(customer);
        when(staffRepo.findFirstByProfile_Account_Username("customer")).thenReturn(Optional.empty());
        assertNull(controller.get(customer.getAccountId()).getBody().systemRole());
    }

    @Test
    void adminCannotManageClinicManager() {
        Account target = account("manager");
        when(accountService.findById(target.getAccountId())).thenReturn(target);
        when(staffRepo.findFirstByProfile_Account_Username("manager"))
                .thenReturn(Optional.of(StaffInfo.builder().systemRole(SystemRole.CLINIC_MANAGER).build()));
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        assertThrows(AccessDeniedException.class, () -> controller.get(target.getAccountId()));
    }

    @Test
    void clinicManagerCannotManageAdminOrAnotherManager() {
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.CLINIC_MANAGER);
        for (SystemRole role : List.of(SystemRole.ADMIN, SystemRole.CLINIC_MANAGER)) {
            Account target = account(role.name());
            when(accountService.findById(target.getAccountId())).thenReturn(target);
            when(staffRepo.findFirstByProfile_Account_Username(role.name()))
                    .thenReturn(Optional.of(StaffInfo.builder().systemRole(role).build()));
            assertThrows(AccessDeniedException.class, () -> controller.get(target.getAccountId()));
        }
    }

    @Test
    void updateResetLockAndDeleteRespectResolvedRoleAndDelegate() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().accountId(id).username("receptionist")
                .role(Role.STAFF).isActive(true).build();
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.CLINIC_MANAGER);
        when(accountService.findById(id)).thenReturn(account);
        when(staffRepo.findFirstByProfile_Account_Username("receptionist"))
                .thenReturn(Optional.of(StaffInfo.builder().systemRole(SystemRole.RECEPTIONIST).build()));
        AccountUpdateRequest update = new AccountUpdateRequest("receptionist", Role.STAFF, true);
        when(accountService.update(id, update)).thenReturn(account);
        assertEquals(SystemRole.RECEPTIONIST, controller.update(id, update).getBody().systemRole());

        assertEquals(204, controller.adminResetPassword(id, Map.of("newPassword", "Secret123"))
                .getStatusCode().value());
        verify(accountService).adminResetPassword(id, "Secret123");

        when(accountService.lock(id)).thenReturn(account);
        assertEquals(SystemRole.RECEPTIONIST, controller.lock(id).getBody().systemRole());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(accountService).softDelete(id);
    }

    private Account account(String username) {
        return Account.builder().accountId(UUID.randomUUID()).username(username)
                .role(Role.STAFF).isActive(true).build();
    }
}
