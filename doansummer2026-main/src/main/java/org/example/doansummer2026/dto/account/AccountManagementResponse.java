package org.example.doansummer2026.dto.account;

import org.example.doansummer2026.enums.SystemRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response cho API quan ly tai khoan - hien thi thong tin cho admin.
 * Format: Ma so, Ho ten/Bo phan, Tai khoan dang nhap, Mat khau cap phat, Vai tro
 */
public record AccountManagementResponse(
    UUID accountId,
    String code,
    String fullNameOrDepartment,
    String username,
    String password,
    String role,
    SystemRole systemRole,
    Boolean isActive,
    LocalDateTime createdAt
) {
    /**
     * Cho khach hang (customer).
     * fullNameOrDepartment se chua ho ten khach hang.
     */
    public static AccountManagementResponse forCustomer(org.example.doansummer2026.model.Account a, String fullName) {
        return new AccountManagementResponse(
            a.getAccountId(),
            a.getAccountId().toString().substring(0, 8),
            fullName,
            a.getUsername(),
            a.getPasswordHash(),
            "CUSTOMER",
            null,
            a.getIsActive(),
            a.getCreatedAt()
        );
    }

    /**
     * Cho nhan su (staff).
     * fullNameOrDepartment se chua "Ho ten - Bo phan" neu co bo phan.
     */
    public static AccountManagementResponse forStaff(org.example.doansummer2026.model.Account a,
                                                     String staffCode,
                                                     String fullName,
                                                     String department,
                                                     SystemRole systemRole) {
        String combinedName = (department != null && !department.isBlank())
                ? fullName + " - " + department
                : fullName;
        return new AccountManagementResponse(
            a.getAccountId(),
            staffCode,
            combinedName,
            a.getUsername(),
            a.getPasswordHash(),
            "STAFF",
            systemRole,
            a.getIsActive(),
            a.getCreatedAt()
        );
    }
}