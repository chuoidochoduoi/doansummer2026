package org.example.doansummer2026.dto.medicalHistory;

import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.model.MedicalRecord;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO cho danh sach lich su kham benh cua benh nhan.
 */
public record MedicalHistoryResponse(
        UUID id,
        UUID visitId,
        String date,
        String time,
        String specialty,
        String doctor,
        String diagnosis,
        String status
) {
    public static MedicalHistoryResponse from(MedicalRecord record) {
        String date = null;
        String time = null;

        if (record.getVisit() != null && record.getVisit().getCheckInTime() != null) {
            date = record.getVisit().getCheckInTime().toLocalDate().toString();
            time = record.getVisit().getCheckInTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        String specialty = null;
        String doctor = null;
        if (record.getVisit() != null && record.getVisit().getAppointment() != null) {
            var appt = record.getVisit().getAppointment();
            // Lấy specialty từ dịch vụ đầu tiên
            if (appt.getServices() != null && !appt.getServices().isEmpty()) {
                specialty = appt.getServices().stream().findFirst()
                        .map(s -> s.getDepartmentType() != null ? s.getDepartmentType().name() : "Khám bệnh")
                        .orElse("Khám bệnh");
            }
        }
        if (specialty == null) {
            if (record.getQueueTicket() == null) specialty = "PARACLINICAL";
            else specialty = "EXAMINATION";
        }

        if (record.getDoctor() != null && record.getDoctor().getProfile() != null && record.getDoctor().getProfile().getFullName() != null) {
            doctor = "BS. " + record.getDoctor().getProfile().getFullName();
        }

        String statusStr = "pending";
        if (record.getStatus() == MedicalRecordStatus.COMPLETED) {
            statusStr = "completed";
        } else if (record.getStatus() == MedicalRecordStatus.DRAFT) {
            statusStr = "draft";
        }

        return new MedicalHistoryResponse(
                record.getRecordId(),
                record.getVisit() != null ? record.getVisit().getVisitId() : null,
                date,
                time,
                specialty,
                doctor,
                record.getDiagnosis(),
                statusStr
        );
    }
}
