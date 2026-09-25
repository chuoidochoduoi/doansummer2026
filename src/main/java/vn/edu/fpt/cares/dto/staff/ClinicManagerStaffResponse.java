package vn.edu.fpt.cares.dto.staff;

import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.model.StaffInfo;

import java.time.LocalDate;
import java.util.UUID;

/** Whitelist thong tin nhan su de Clinic Manager chi co quyen xem. */
public record ClinicManagerStaffResponse(
        UUID staffId, String staffCode, String fullName, LocalDate dateOfBirth, Gender gender,
        String phone, String email, String address, SystemRole systemRole,
        String specializationName, String departmentName, String highestDegree, String university, String status
) {
    public static ClinicManagerStaffResponse from(StaffInfo staff) {
        var profile = staff.getProfile();
        return new ClinicManagerStaffResponse(
                staff.getStaffId(), staff.getStaffCode(), profile != null ? profile.getFullName() : null,
                profile != null ? profile.getDateOfBirth() : null, profile != null ? profile.getGender() : null,
                profile != null ? profile.getPhone() : null, profile != null ? profile.getEmail() : null,
                profile != null ? profile.getAddress() : null, staff.getSystemRole(),
                staff.getSpecialization() != null ? staff.getSpecialization().getName() : null,
                staff.getDepartment() != null ? staff.getDepartment().getName() : null,
                staff.getHighestDegree(), staff.getUniversity(),
                profile != null && profile.getAccount() != null && Boolean.TRUE.equals(profile.getAccount().getIsActive())
                        ? "ACTIVE" : "INACTIVE");
    }
}
