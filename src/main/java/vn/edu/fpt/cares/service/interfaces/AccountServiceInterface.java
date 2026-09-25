package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.account.AccountManagementResponse;
import vn.edu.fpt.cares.dto.account.AccountResponse;
import vn.edu.fpt.cares.dto.account.AccountUpdateRequest;
import vn.edu.fpt.cares.enums.Role;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.model.Account;
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
    PageResponse<AccountManagementResponse> listStaff(String search, SystemRole systemRole, Pageable pageable);
    PageResponse<AccountManagementResponse> listCustomers(String search, String status, Pageable pageable);
}



