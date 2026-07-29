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
        List<AppointmentSummary> appointments,
        List<TestResultSummary> testResults
) {
    public static ProfileCustomerResponse from(Profile profile, Account account,
                                              List<AppointmentSummary> appointments,
                                              List<TestResultSummary> testResults) {
        List<String> allergyList = null;
        if (profile.getAllergies() != null && !profile.getAllergies().isBlank()) {
            allergyList = List.of(profile.getAllergies().split(";"));
        }

        String customerCode = profile.getPhone(); // phone lam ma khach hang

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
                allergyList,
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