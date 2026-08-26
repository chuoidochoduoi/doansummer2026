package org.example.doansummer2026.dto.profile;

import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.Account;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response cho API profile cua customer.
 */
public record ProfileCustomerResponse(
        UUID accountId,
        String customerCode,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        String address,
        BloodType bloodType,
        String insuranceId,
        Integer height,
        Integer weight,
        List<String> allergies,
        org.example.doansummer2026.enums.AllergyStatus allergyStatus,
        List<AppointmentSummary> appointments,
        List<TestResultSummary> testResults
) {
    public static ProfileCustomerResponse from(Profile profile, Account account,
                                              List<AppointmentSummary> appointments,
                                              List<TestResultSummary> testResults) {
        var allergy = org.example.doansummer2026.dto.medicalRecord.PatientAllergyResponse.from(profile);

        String customerCode = profile.getPatientCode();

        return new ProfileCustomerResponse(
                account.getAccountId(),
                customerCode,
                profile.getFullName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getPhone(),
                profile.getEmail(),
                profile.getAddress(),
                profile.getBloodType(),
                profile.getInsuranceId(),
                profile.getHeight(),
                profile.getWeight(),
                allergy.items(),
                allergy.status(),
                appointments,
                testResults
        );
    }

    public record AppointmentSummary(
            String date,
            String doctor,
            String specialty,
            String status
    ) {}

    public record TestResultSummary(
            String name,
            String date
    ) {}
}
