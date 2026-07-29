package org.example.doansummer2026.dto.staff;

import java.util.UUID;

/**
 * Response tra danh sach bac si khi chon head doctor cho Department.
 * Chi bao staffId, staffCode, fullName, systemRole, specialization.
 */
public record StaffOptionResponse(
        UUID staffId,
        String staffCode,
        String fullName,
        SystemRoleBrief systemRole,
        String specializationName
) {
    public enum SystemRoleBrief {
        GENERAL_DOCTOR, SPECIALIST_DOCTOR, NURSE, RECEPTIONIST, CASHIER, CLINIC_MANAGER, ADMIN
    }

    public static StaffOptionResponse from(org.example.doansummer2026.model.StaffInfo s) {
        String specName = s.getSpecialization() != null ? s.getSpecialization().getName() : null;
        String fullName = s.getProfile() != null ? s.getProfile().getFullName() : null;
        return new StaffOptionResponse(
                s.getStaffId(),
                s.getStaffCode(),
                fullName,
                SystemRoleBrief.valueOf(s.getSystemRole().name()),
                specName
        );
    }
}