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
                        .map(s -> s.getCategory() != null ? s.getCategory().getName() : "Khám bệnh")
                        .orElse("Khám bệnh");
            }
            // Doctor từ medicalRecord
            if (record.getDoctor() != null) {
                doctor = "BS. " + record.getDoctor().getStaffCode();
            }
        }

        String statusStr = "pending";
        if (record.getStatus() == MedicalRecordStatus.COMPLETED) {
            statusStr = "completed";
        } else if (record.getStatus() == MedicalRecordStatus.DRAFT) {
            statusStr = "draft";
        }

        return new MedicalHistoryResponse(
                record.getRecordId(),
                date,
                time,
                specialty,
                doctor,
                record.getDiagnosis(),
                statusStr
        );
    }
}