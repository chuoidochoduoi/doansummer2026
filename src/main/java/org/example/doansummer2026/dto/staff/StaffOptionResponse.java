package org.example.doansummer2026.dto.staff;

import java.util.UUID;
import java.util.List;

/**
 * Response tra danh sach bac si khi chon head doctor cho Department.
 * Chi bao staffId, staffCode, fullName, systemRole, specialization.
 */
public record StaffOptionResponse(
        UUID staffId,
        String staffCode,
        String fullName,
        String avatarUrl,
        SystemRoleBrief systemRole,
        String specializationName,
        UUID assignedDepartmentId,
        UUID specializationId,
        List<UUID> capabilityIds
) {
    public enum SystemRoleBrief {
        DOCTOR, NURSE, RECEPTIONIST, CASHIER, CLINIC_MANAGER, ADMIN
    }

    public static StaffOptionResponse from(org.example.doansummer2026.model.StaffInfo s) {
        return from(s, null);
    }

    public static StaffOptionResponse from(org.example.doansummer2026.model.StaffInfo s, UUID assignedDepartmentId) {
        return from(s, assignedDepartmentId, List.of());
    }

    public static StaffOptionResponse from(org.example.doansummer2026.model.StaffInfo s,
                                           UUID assignedDepartmentId,
                                           List<UUID> capabilityIds) {
        String specName = s.getSpecialization() != null ? s.getSpecialization().getName() : null;
        String fullName = s.getProfile() != null ? s.getProfile().getFullName() : null;
        String avatarUrl = s.getProfile() != null ? s.getProfile().getAvatarUrl() : null;
        return new StaffOptionResponse(
                s.getStaffId(),
                s.getStaffCode(),
                fullName,
                avatarUrl,
                SystemRoleBrief.valueOf(s.getSystemRole().normalized().name()),
                specName,
                assignedDepartmentId,
                s.getSpecialization() != null ? s.getSpecialization().getSpecializationId() : null,
                capabilityIds == null ? List.of() : capabilityIds
        );
    }
}
