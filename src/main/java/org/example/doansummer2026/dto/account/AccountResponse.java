package org.example.doansummer2026.dto.account;

import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String username,
        Role role,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getAccountId(), a.getUsername(), a.getRole(),
                a.getIsActive(), a.getCreatedAt());
    }
}