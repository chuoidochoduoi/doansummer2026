package org.example.doansummer2026.dto.medicalRecord;

import org.example.doansummer2026.dto.icd.ICD10SelectionResponse;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsResponse;
import org.example.doansummer2026.dto.medicalRecord.PrescriptionItemResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestResponse;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
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
        VitalSignsResponse vitalSigns,
        Set<PrescriptionItemResponse> prescriptionItems,
        Set<TestRequestResponse> testRequests,
        Set<ICD10SelectionResponse> icdSelections,
        Integer ratingScore,
        LocalDateTime ratedAt
) {
    public static MedicalRecordResponse from(MedicalRecord r, boolean includeNested) {
        UUID visitId = r.getVisit() != null ? r.getVisit().getVisitId() : null;
        UUID doctorId = r.getDoctor() != null ? r.getDoctor().getStaffId() : null;
        String doctorName = r.getDoctor() != null ? r.getDoctor().getStaffCode() : null;
        VitalSignsResponse vitalSigns = r.getVitalSigns() != null ? VitalSignsResponse.from(r.getVitalSigns()) : null;
        Set<PrescriptionItemResponse> prescriptions = includeNested && r.getPrescriptionItems() != null
                ? r.getPrescriptionItems().stream().map(PrescriptionItemResponse::from).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>();
        Set<TestRequestResponse> testRequests = includeNested && r.getTestRequests() != null
                ? r.getTestRequests().stream().map(TestRequestResponse::from).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>();
        Set<ICD10SelectionResponse> icdSelections = includeNested && r.getIcdSelections() != null
                ? r.getIcdSelections().stream().map(ICD10SelectionResponse::from).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>();
        return new MedicalRecordResponse(r.getRecordId(), visitId, doctorId, doctorName,
                r.getChiefComplaint(), r.getClinicalFindings(), r.getDiagnosis(),
                r.getPrescriptionNote(), r.getConclusion(), r.getPatientInstruction(),
                r.getStatus(), r.getCompletedAt(), r.getCreatedAt(),
                vitalSigns, prescriptions, testRequests, icdSelections,
                r.getRatingScore(), r.getRatedAt());
    }
}




