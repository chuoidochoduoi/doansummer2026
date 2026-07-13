package org.example.doansummer2026.dto.medicalRecord;

import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MedicalRecordResponse(
        UUID recordId,
        UUID visitId,
        UUID doctorId,
        String doctorName,
        String chiefComplaint,
        String clinicalFindings,
        String diagnosis,
        String prescriptionNote,
        String conclusion,
        String patientInstruction,
        MedicalRecordStatus status,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        VitalSignsResponse vitalSigns,  // Thong tin chi tiet vital signs
        List<UUID> testRequestIds
) {
    public static MedicalRecordResponse from(MedicalRecord r, boolean includeNested) {
        UUID visitId = r.getVisit() != null ? r.getVisit().getVisitId() : null;
        UUID doctorId = r.getDoctor() != null ? r.getDoctor().getStaffId() : null;
        String doctorName = r.getDoctor() != null ? r.getDoctor().getStaffCode() : null;
        VitalSignsResponse vitalSigns = r.getVitalSigns() != null ? VitalSignsResponse.from(r.getVitalSigns()) : null;
        List<UUID> testIds = includeNested
                ? r.getTestRequests().stream().map(t -> t.getTestRequestId()).toList()
                : List.of();
        return new MedicalRecordResponse(r.getRecordId(), visitId, doctorId, doctorName,
                r.getChiefComplaint(), r.getClinicalFindings(), r.getDiagnosis(),
                r.getPrescriptionNote(), r.getConclusion(), r.getPatientInstruction(),
                r.getStatus(), r.getCompletedAt(), r.getCreatedAt(),
                vitalSigns, testIds);
    }
}
