package vn.edu.fpt.cares.dto.medicalrecord;

import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.model.MedicalRecord;

import java.time.LocalDate;
import java.util.UUID;

public record FollowUpResponse(
        UUID recordId,
        String recordCode,
        UUID customerId,
        String customerCode,
        String customerName,
        String customerPhone,
        Gender customerGender,
        Integer customerAge,
        UUID doctorId,
        String doctorName,
        String followUpNote,
        LocalDate followUpDate
) {
    public static FollowUpResponse from(MedicalRecord r) {
        var customer = r.getVisit().getCustomer();
        return new FollowUpResponse(
                r.getRecordId(),
                r.getRecordCode(),
                customer != null ? customer.getProfileId() : null,
                customer != null ? customer.getPatientCode() : null,
                customer != null ? customer.getFullName() : (r.getVisit().getAppointment() != null ? r.getVisit().getAppointment().getGuestFullName() : "Khách vãng lai"),
                customer != null ? customer.getPhone() : (r.getVisit().getAppointment() != null ? r.getVisit().getAppointment().getGuestPhone() : ""),
                customer != null ? customer.getGender() : null,
                null, // age calculated later if needed
                r.getDoctor() != null ? r.getDoctor().getStaffId() : null,
                r.getDoctor() != null && r.getDoctor().getProfile() != null ? r.getDoctor().getProfile().getFullName() : "Unknown",
                r.getFollowUpNote(),
                r.getFollowUpDate()
        );
    }
}
