package org.example.doansummer2026.dto.family;

import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.AllergyStatus;
import org.example.doansummer2026.enums.FamilyRelationship;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.FamilyMember;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

public record FamilyMemberResponse(
        UUID id,
        UUID patientProfileId,
        String patientCode,
        String fullName,
        LocalDate dateOfBirth,
        Integer age,
        Gender gender,
        FamilyRelationship relationship,
        String relationshipName,
        String address,
        BloodType bloodType,
        AllergyStatus allergyStatus,
        List<String> allergies,
        boolean active,
        String contactPhone,
        String contactEmail
) {
    public static FamilyMemberResponse from(FamilyMember relation) {
        var member = relation.getMemberProfile();
        var owner = relation.getOwnerProfile();
        var allergy = org.example.doansummer2026.dto.medicalRecord.PatientAllergyResponse.from(member);
        Integer age = member.getDateOfBirth() == null ? null
                : Period.between(member.getDateOfBirth(), LocalDate.now()).getYears();
        return new FamilyMemberResponse(
                relation.getFamilyMemberId(), member.getProfileId(), member.getPatientCode(),
                member.getFullName(), member.getDateOfBirth(), age, member.getGender(),
                relation.getRelationship(), relation.getRelationship().getDisplayName(),
                member.getAddress(), member.getBloodType(), allergy.status(), allergy.items(),
                Boolean.TRUE.equals(relation.getIsActive()), owner.getPhone(), owner.getEmail()
        );
    }
}
