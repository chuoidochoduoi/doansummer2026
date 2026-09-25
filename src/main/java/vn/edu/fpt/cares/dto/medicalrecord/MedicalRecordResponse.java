package vn.edu.fpt.cares.dto.medicalrecord;

import vn.edu.fpt.cares.dto.icd.ICD10SelectionResponse;
import vn.edu.fpt.cares.model.MedicalRecord;
import vn.edu.fpt.cares.enums.MedicalRecordStatus;
import vn.edu.fpt.cares.dto.vitalsigns.VitalSignsResponse;
import vn.edu.fpt.cares.dto.medicalrecord.PrescriptionItemResponse;
import vn.edu.fpt.cares.dto.testrequest.TestRequestResponse;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record MedicalRecordResponse(
        UUID recordId,
        String recordCode,
        UUID visitId,
        UUID doctorId,
        String doctorName,
        String chiefComplaint,
        String clinicalFindings,
        String diagnosis,
        String prescriptionNote,
        String conclusion,
        String patientInstruction,
        String followUpNote,
        java.time.LocalDate followUpDate,
        UUID followUpAppointmentId,
        MedicalRecordStatus status,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        VitalSignsResponse vitalSigns,
        Set<PrescriptionItemResponse> prescriptionItems,
        Set<TestRequestResponse> testRequests,
        Set<ICD10SelectionResponse> icdSelections,
        Integer ratingScore,
        LocalDateTime ratedAt
        ,Long version
        ,UUID nursingUpdatedById
        ,String nursingUpdatedByName
        ,LocalDateTime nursingUpdatedAt
        ,UUID doctorConfirmedById
        ,String doctorConfirmedByName
        ,LocalDateTime doctorConfirmedAt
        ,JsonNode specialtyData
        ,vn.edu.fpt.cares.dto.clinicalform.ResolvedClinicalFormResponse clinicalForm
) {
    public static MedicalRecordResponse from(MedicalRecord r, boolean includeNested) {
        UUID visitId = r.getVisit() != null ? r.getVisit().getVisitId() : null;
        UUID doctorId = r.getDoctor() != null ? r.getDoctor().getStaffId() : null;
        String doctorName = r.getDoctor() != null && r.getDoctor().getProfile() != null ? r.getDoctor().getProfile().getFullName() : null;
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
        return new MedicalRecordResponse(r.getRecordId(), r.getRecordCode(), visitId, doctorId, doctorName,
                r.getChiefComplaint(), r.getClinicalFindings(), r.getDiagnosis(),
                r.getPrescriptionNote(), r.getConclusion(), r.getPatientInstruction(),
                r.getFollowUpNote(), r.getFollowUpDate(),
                r.getFollowUpAppointment() == null ? null : r.getFollowUpAppointment().getAppointmentId(),
                r.getStatus(), r.getCompletedAt(), r.getCreatedAt(),
                vitalSigns, prescriptions, testRequests, icdSelections,
                r.getRatingScore(), r.getRatedAt(), r.getVersion(),
                r.getNursingUpdatedBy()!=null?r.getNursingUpdatedBy().getStaffId():null,
                r.getNursingUpdatedBy()!=null&&r.getNursingUpdatedBy().getProfile()!=null?r.getNursingUpdatedBy().getProfile().getFullName():null,
                r.getNursingUpdatedAt(),
                r.getDoctorConfirmedBy()!=null?r.getDoctorConfirmedBy().getStaffId():null,
                r.getDoctorConfirmedBy()!=null&&r.getDoctorConfirmedBy().getProfile()!=null?r.getDoctorConfirmedBy().getProfile().getFullName():null,
                r.getDoctorConfirmedAt(),
                r.getSpecialtyData(), includeNested ? resolvedClinicalForm(r) : null);
    }

    private static vn.edu.fpt.cares.dto.clinicalform.ResolvedClinicalFormResponse resolvedClinicalForm(
            MedicalRecord record) {
        return null;
    }
}
