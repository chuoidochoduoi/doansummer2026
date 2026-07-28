package org.example.doansummer2026.dto.profile;

import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Profile;

import java.time.LocalDate;
import java.util.UUID;

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
        BloodType bloodType,
        Boolean hasStaffInfo,
        UUID staffId
) {
    public static ProfileResponse from(Profile p) {
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
                p.getBloodType(),
                null,
                null
        );
    }
}



