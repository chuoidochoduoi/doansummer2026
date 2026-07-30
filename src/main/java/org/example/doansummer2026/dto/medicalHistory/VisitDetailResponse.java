package org.example.doansummer2026.dto.medicalHistory;

import org.example.doansummer2026.model.Icd10Selection;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.VitalSigns;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho chi tiet luot kham.
 */
public record VisitDetailResponse(
        UUID id,
        String recordId,
        String appointmentDate,
        String symptoms,
        String clinicalResult,
        List<DiagnosisResponse> diagnoses,
        String treatmentPlan,
        String followUpNote,
        String prescription,
        List<TestResponse> tests,
        String status,
        Integer ratingScore,
        String doctorName,
        List<String> labDoctors
) {
    public static VisitDetailResponse from(MedicalRecord record) {
        String appointmentDate = record.getVisit() != null && record.getVisit().getCheckInTime() != null
                ? record.getVisit().getCheckInTime().toLocalDate().toString() : null;

        String symptoms = record.getChiefComplaint();

        String clinicalResult = null;
        if (record.getVitalSigns() != null) {
            VitalSigns v = record.getVitalSigns();
            var sb = new StringBuilder();
            if (v.getBloodPressure() != null) sb.append("Huyết áp: ").append(v.getBloodPressure()).append(" ");
            if (v.getHeartRate() != null) sb.append("Nhịp tim: ").append(v.getHeartRate()).append(" ");
            if (v.getTemperature() != null) sb.append("Nhiệt độ: ").append(v.getTemperature()).append("°C ");
            if (v.getWeight() != null) sb.append("Cân nặng: ").append(v.getWeight()).append("kg ");
            clinicalResult = sb.length() > 0 ? sb.toString().trim() : null;
        }

        List<DiagnosisResponse> diagnoses = record.getIcdSelections() != null
                ? record.getIcdSelections().stream().map(DiagnosisResponse::from).toList()
                : List.of();

        String treatmentPlan = record.getConclusion();
        String followUpNote = record.getPatientInstruction();

        String prescription = record.getPrescriptionItems() != null && !record.getPrescriptionItems().isEmpty()
                ? record.getPrescriptionItems().stream()
                        .map(p -> p.getMedicineName() + " " + p.getQuantity() + " " + (p.getUnit() != null ? p.getUnit() : ""))
                        .reduce((a, b) -> a + "; " + b)
                        .orElse(null)
                : null;

        // Tests chưa có trong mô hình - trả về rỗng
        List<TestResponse> tests = List.of();

        String status = record.getStatus() != null ? record.getStatus().name() : null;
        Integer ratingScore = record.getRatingScore();
        String doctorName = record.getDoctor() != null && record.getDoctor().getProfile() != null 
                ? record.getDoctor().getProfile().getFullName() : null;
        
        List<String> labDoctors = record.getTestRequests() != null
                ? record.getTestRequests().stream()
                        .filter(tr -> tr.getTestResult() != null && tr.getTestResult().getPerformedBy() != null && tr.getTestResult().getPerformedBy().getProfile() != null)
                        .map(tr -> tr.getTestResult().getPerformedBy().getProfile().getFullName())
                        .distinct()
                        .toList()
                : List.of();

        return new VisitDetailResponse(
                record.getRecordId(),
                record.getRecordCode(),
                appointmentDate,
                symptoms,
                clinicalResult,
                diagnoses,
                treatmentPlan,
                followUpNote,
                prescription,
                tests,
                status,
                ratingScore,
                doctorName,
                labDoctors
        );
    }
}