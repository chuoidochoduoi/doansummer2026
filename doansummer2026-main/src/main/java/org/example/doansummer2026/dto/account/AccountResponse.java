package org.example.doansummer2026.dto.account;

import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;

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



