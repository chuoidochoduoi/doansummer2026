package vn.edu.fpt.cares.dto.account;

import vn.edu.fpt.cares.enums.Role;

public record AccountUpdateRequest(
        String username,
        Role role,
        Boolean isActive
) {}



