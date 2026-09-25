package vn.edu.fpt.cares.dto.profile;

import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.model.Account;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response cho API profile cua customer.
 */
public record ProfileCustomerResponse(
        UUID profileId,
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
        vn.edu.fpt.cares.enums.AllergyStatus allergyStatus,
        List<AppointmentSummary> appointments,
        List<TestResultSummary> testResults
) {
    public static ProfileCustomerResponse from(Profile profile, Account account,
                                              List<AppointmentSummary> appointments,
                                              List<TestResultSummary> testResults) {
        var allergy = vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse.from(profile);

        String customerCode = profile.getPatientCode();

        return new ProfileCustomerResponse(
                profile.getProfileId(),
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
