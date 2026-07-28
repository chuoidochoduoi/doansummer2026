package org.example.doansummer2026.dto.account;

import org.example.doansummer2026.enums.Role;

public record AccountUpdateRequest(
        String username,
        Role role,
        Boolean isActive
) {}



