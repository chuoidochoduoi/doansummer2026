package vn.edu.fpt.cares.dto.account;

import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.enums.Role;
import vn.edu.fpt.cares.enums.SystemRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String username,
        Role role,
        SystemRole systemRole,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account a, SystemRole systemRole) {
        return new AccountResponse(a.getAccountId(), a.getUsername(), a.getRole(),
                systemRole, a.getIsActive(), a.getCreatedAt());
    }
}



