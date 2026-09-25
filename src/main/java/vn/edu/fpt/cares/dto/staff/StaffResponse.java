package vn.edu.fpt.cares.dto.staff;

import vn.edu.fpt.cares.dto.profile.ProfileResponse;
import vn.edu.fpt.cares.dto.specialization.SpecializationResponse;
import vn.edu.fpt.cares.model.StaffInfo;

import java.util.UUID;

public record StaffResponse(
        UUID staffId,
        String staffCode,
        String nationalId,
        String licenseNumber,
        String highestDegree,
        String university,
        SystemRoleBrief systemRole,
        ProfileResponse profile,
        SpecializationResponse specialization
) {
    public enum SystemRoleBrief {
        DOCTOR, NURSE, RECEPTIONIST, CASHIER, CLINIC_MANAGER, ADMIN
    }

    public static StaffResponse from(StaffInfo s, ProfileResponse p,
                                     SpecializationResponse sp) {
        return new StaffResponse(
                s.getStaffId(),
                s.getStaffCode(),
                s.getNationalId(),
                s.getLicenseNumber(),
                s.getHighestDegree(),
                s.getUniversity(),
                SystemRoleBrief.valueOf(s.getSystemRole().normalized().name()),
                p,
                sp
        );
    }
}


