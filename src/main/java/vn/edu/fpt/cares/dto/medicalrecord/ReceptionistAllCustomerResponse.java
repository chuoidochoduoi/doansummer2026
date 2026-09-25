package vn.edu.fpt.cares.dto.medicalrecord;

import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Thong tin customer hoac guest cho receptionist.
 * Gom ca customer co account va guest van g lai.
 */
public record ReceptionistAllCustomerResponse(
        UUID customerId,           // UUID neu co account, hoac UUID cua appointment (dung lam id tam thoi)
        String patientCode,        // Ma benh nhan rieng, khong phai so dien thoai
        String fullName,
        Gender gender,
        LocalDate dateOfBirth,
        BloodType bloodType,
        String phone,
        String email,
        String address,
        boolean isGuest           // true neu la guest vang lai
) {
    public static ReceptionistAllCustomerResponse forRegistered(
            UUID profileId, String patientCode, String phone, String fullName, Gender gender,
            LocalDate dateOfBirth, BloodType bloodType, String email, String address) {
        return new ReceptionistAllCustomerResponse(
                profileId, patientCode, fullName, gender, dateOfBirth, bloodType,
                phone, email, address, false
        );
    }

    public static ReceptionistAllCustomerResponse forGuest(
            UUID profileId, String patientCode, String phone, String fullName, Gender gender,
            LocalDate dateOfBirth, BloodType bloodType, String email, String address) {
        return new ReceptionistAllCustomerResponse(
                profileId, patientCode, fullName, gender, dateOfBirth, bloodType,
                phone, email, address, true
        );
    }
}
