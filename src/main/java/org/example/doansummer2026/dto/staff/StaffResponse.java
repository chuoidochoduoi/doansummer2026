package org.example.doansummer2026.dto.staff;

import org.example.doansummer2026.dto.department.DepartmentResponse;
import org.example.doansummer2026.dto.profile.ProfileResponse;
import org.example.doansummer2026.dto.specialization.SpecializationResponse;
import org.example.doansummer2026.model.StaffInfo;

import java.util.UUID;

public record StaffResponse(
        UUID staffId,
        String staffCode,
        String nationalId,
        String licenseNumber,
        SystemRoleBrief systemRole,
        ProfileResponse profile,
        DepartmentResponse department,
        SpecializationResponse specialization
) {
    public enum SystemRoleBrief {
        GENERAL_DOCTOR, SPECIALIST_DOCTOR, NURSE, RECEPTIONIST, CASHIER, CLINIC_MANAGER
    }

    public static StaffResponse from(StaffInfo s, ProfileResponse p,
                                     DepartmentResponse d, SpecializationResponse sp) {
        return new StaffResponse(
                s.getStaffId(),
                s.getStaffCode(),
                s.getNationalId(),
                s.getLicenseNumber(),
                SystemRoleBrief.valueOf(s.getSystemRole().name()),
                p,
                d,
                sp
        );
    }
}