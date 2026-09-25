package vn.edu.fpt.cares.dto.profile;

import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.model.Profile;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public record ProfileResponse(
        UUID profileId,
        UUID accountId,
        String username,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        String address,
        String avatarUrl,
        BloodType bloodType,
        String insuranceId,
        Integer height,
        Integer weight,
        List<String> allergies,
        vn.edu.fpt.cares.enums.AllergyStatus allergyStatus,
        Boolean hasStaffInfo,
        UUID staffId
) {
    public static ProfileResponse from(Profile p) {
        if (p == null) return null;
        boolean hasStaff = p.getAccount() != null;
        return new ProfileResponse(
                p.getProfileId(),
                hasStaff ? p.getAccount().getAccountId() : null,
                hasStaff ? p.getAccount().getUsername() : null,
                p.getFullName(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getPhone(),
                p.getEmail(),
                p.getAddress(),
                p.getAvatarUrl(),
                p.getBloodType(),
                p.getInsuranceId(),
                p.getHeight(),
                p.getWeight(),
                vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse.from(p).items(),
                vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse.from(p).status(),
                null,
                null
        );
    }
}



