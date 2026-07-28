package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.account.AccountManagementResponse;
import org.example.doansummer2026.dto.account.AccountResponse;
import org.example.doansummer2026.dto.account.AccountUpdateRequest;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.Account;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Service interface for Account management. */
public interface AccountServiceInterface {
    Account create(String username, String rawPassword, Role role);
    Account findById(UUID id);
    Account findByUsername(String username);
    Account update(UUID id, AccountUpdateRequest req);
    Account lock(UUID id);
    void changePassword(UUID id, String oldRaw, String newRaw);
    void softDelete(UUID id);
    PageResponse<AccountResponse> list(Role role, Pageable pageable);
    PageResponse<AccountManagementResponse> listStaff(SystemRole systemRole, Pageable pageable);
    PageResponse<AccountManagementResponse> listCustomers(Pageable pageable);
}



