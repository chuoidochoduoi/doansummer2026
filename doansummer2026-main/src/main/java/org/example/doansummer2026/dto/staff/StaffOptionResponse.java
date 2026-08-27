package org.example.doansummer2026.dto.staff;

import java.util.UUID;

/**
 * Response tra danh sach bac si khi chon head doctor cho Department hoac hien thi tren Landing Page.
 * Chi bao staffId, staffCode, fullName, avatarUrl, systemRole, specialization.
 */
public record StaffOptionResponse(
        UUID staffId,
        String staffCode,
        String fullName,
        String avatarUrl, // Bổ sung trường avatarUrl riêng biệt
        SystemRoleBrief systemRole,
        String specializationName,
        UUID assignedDepartmentId
) {
    public enum SystemRoleBrief {
        DOCTOR, NURSE, RECEPTIONIST, CASHIER, CLINIC_MANAGER, ADMIN
    }

    public static StaffOptionResponse from(org.example.doansummer2026.model.StaffInfo s) {
        return from(s, null);
    }

    public static StaffOptionResponse from(org.example.doansummer2026.model.StaffInfo s, UUID assignedDepartmentId) {
        String specName = s.getSpecialization() != null ? s.getSpecialization().getName() : null;
        String fullName = s.getProfile() != null ? s.getProfile().getFullName() : null;
        String avatarUrl = s.getProfile() != null ? s.getProfile().getAvatarUrl() : null;
        return new StaffOptionResponse(
                s.getStaffId(),
                s.getStaffCode(),
                fullName,
                avatarUrl, // Map avatarUrl từ Profile
                SystemRoleBrief.valueOf(s.getSystemRole().normalized().name()),
                specName,
                assignedDepartmentId
        );
    }
}