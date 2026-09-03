package org.example.doansummer2026.dto.medicalrecord;

import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Profile;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

/**
 * Thong tin customer (benh nhan) cho receptionist.
 * Tra ve danh sach customer khong lop lai.
 */
public record ReceptionistCustomerResponse(
        UUID customerId,
        String patientCode,
        String fullName,
        Gender gender,
        LocalDate dateOfBirth,
        BloodType bloodType,
        String phone,
        String email,
        String address,
        org.example.doansummer2026.enums.AllergyStatus allergyStatus,
        List<String> allergies
) {
    public static ReceptionistCustomerResponse from(Profile p) {
        var allergy = PatientAllergyResponse.from(p);
        return new ReceptionistCustomerResponse(
                p.getProfileId(),
                p.getPatientCode(),
                p.getFullName(),
                p.getGender(),
                p.getDateOfBirth(),
                p.getBloodType(),
                p.getPhone(),
                p.getEmail(),
                p.getAddress(),
                allergy.status(),
                allergy.items()
        );
    }
}
