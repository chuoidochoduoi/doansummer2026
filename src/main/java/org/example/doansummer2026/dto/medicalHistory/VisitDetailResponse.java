package org.example.doansummer2026.dto.medicalHistory;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.PrescriptionItem;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.model.VitalSigns;

import java.util.List;
import java.util.UUID;

/** DTO chi tiet cua mot CustomerVisit, gom nhieu ho so kham va yeu cau CLS. */
public record VisitDetailResponse(
        UUID id,
        UUID visitId,
        String visitCode,
        String patientName,
        String patientDateOfBirth,
        String patientGender,
        String patientPhone,
        String patientAddress,
        String recordId,
        String appointmentDate,
        String checkInTime,
        String symptoms,
        String clinicalResult,
        List<DiagnosisResponse> diagnoses,
        String treatmentPlan,
        String followUpNote,
        String prescription,
        List<TestResponse> tests,
        String status,
        Integer ratingScore,
        String ratingComment,
        String ratedAt,
        String feedbackStatus,
        String managerResponse,
        String respondedAt,
        String respondedByName,
        String doctorName,
        List<String> labDoctors,
        List<ExaminationResponse> examinations
) {
    public record PrescriptionItemResponse(String medicineName, Integer quantity, String unit,
                                           String note, Integer frequencyPerDay) {
        private static PrescriptionItemResponse from(PrescriptionItem item) {
            return new PrescriptionItemResponse(item.getMedicineName(), item.getQuantity(), item.getUnit(),
                    item.getNote(), item.getFrequencyPerDay());
        }
    }

    public record ExaminationResponse(UUID recordId, String recordCode, String serviceName, UUID doctorId,
                                      String doctorName, String status, String startedAt, String completedAt,
                                      String chiefComplaint, String symptoms, String clinicalFindings,
                                      String clinicalResult, String diagnosis, List<DiagnosisResponse> diagnoses,
                                      String conclusion, String treatmentPlan, String patientInstruction,
                                      String followUpNote, String prescription,
                                      List<PrescriptionItemResponse> prescriptionItems) {}

    public static VisitDetailResponse from(List<MedicalRecord> records, List<TestRequest> testRequests) {
        if (records == null || records.isEmpty()) return null;

        MedicalRecord first = records.get(0);
        MedicalRecord feedbackRecord = records.stream()
                .filter(r -> r.getRatingScore() != null || r.getFeedbackStatus() != null)
                .findFirst()
                .orElse(records.get(records.size() - 1));

        List<ExaminationResponse> examinations = records.stream()
                // Standalone record chi la noi luu TestRequest; khong phai mot lan kham.
                .filter(r -> r.getQueueTicket() != null
                        && r.getQueueTicket().getDepartment() != null
                        && r.getQueueTicket().getDepartment().getDepartmentType() == DepartmentType.EXAMINATION)
                .map(VisitDetailResponse::examinationFrom)
                .toList();

        List<TestResponse> tests = testRequests == null ? List.of()
                : testRequests.stream().map(VisitDetailResponse::testFrom).toList();
        List<String> labDoctors = tests.stream()
                .map(TestResponse::performedBy)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();

        // Standalone record CLS la ban ghi ky thuat, khong duoc lam luot kham
        // bi ket o IN_PROGRESS sau khi tat ca ket qua CLS da hoan thanh.
        boolean examinationsCompleted = examinations.stream()
                .allMatch(exam -> MedicalRecordStatus.COMPLETED.name().equals(exam.status()));
        boolean testsCompleted = tests.stream().allMatch(test -> "COMPLETED".equals(test.status())
                || "CANCELLED".equals(test.status()));
        boolean completed = (!examinations.isEmpty() || !tests.isEmpty())
                && examinationsCompleted && testsCompleted;
        String appointmentDate = first.getVisit() != null && first.getVisit().getCheckInTime() != null
                ? first.getVisit().getCheckInTime().toLocalDate().toString() : null;
        String checkInTime = first.getVisit() != null && first.getVisit().getCheckInTime() != null
                ? first.getVisit().getCheckInTime().toString() : null;
        UUID visitId = first.getVisit() != null ? first.getVisit().getVisitId() : null;

        return new VisitDetailResponse(
                first.getRecordId(),
                visitId,
                // He thong chua co visitCode luu rieng; dung ma ho so dau tien
                // de hien thi mot ma de nhan biet, khong tao ma gia tu UUID.
                first.getRecordCode(),
                first.getVisit() != null && first.getVisit().getCustomer() != null ? first.getVisit().getCustomer().getFullName() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null && first.getVisit().getCustomer().getDateOfBirth() != null ? first.getVisit().getCustomer().getDateOfBirth().toString() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null && first.getVisit().getCustomer().getGender() != null ? first.getVisit().getCustomer().getGender().name() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null ? first.getVisit().getCustomer().getPhone() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null ? first.getVisit().getCustomer().getAddress() : null,
                first.getRecordCode(),
                appointmentDate,
                checkInTime,
                first.getChiefComplaint(),
                clinicalResultFrom(first),
                diagnosesFrom(first),
                first.getConclusion(),
                first.getPatientInstruction(),
                prescriptionTextFrom(first),
                tests,
                completed ? "COMPLETED" : "IN_PROGRESS",
                feedbackRecord.getRatingScore(),
                feedbackRecord.getRatingComment(),
                feedbackRecord.getRatedAt() != null ? feedbackRecord.getRatedAt().toString() : null,
                feedbackRecord.getFeedbackStatus(),
                feedbackRecord.getManagerResponse(),
                feedbackRecord.getRespondedAt() != null ? feedbackRecord.getRespondedAt().toString() : null,
                feedbackRecord.getRespondedBy() != null && feedbackRecord.getRespondedBy().getProfile() != null
                        ? feedbackRecord.getRespondedBy().getProfile().getFullName() : null,
                doctorName(first),
                labDoctors,
                examinations
        );
    }

    private static ExaminationResponse examinationFrom(MedicalRecord record) {
        List<PrescriptionItemResponse> prescriptionItems = record.getPrescriptionItems() == null ? List.of()
                : record.getPrescriptionItems().stream().map(PrescriptionItemResponse::from).toList();
        return new ExaminationResponse(
                record.getRecordId(), record.getRecordCode(),
                record.getQueueTicket().getService() != null ? record.getQueueTicket().getService().getName() : "Khám bệnh",
                record.getDoctor() != null ? record.getDoctor().getStaffId() : null,
                doctorName(record), record.getStatus() != null ? record.getStatus().name() : null,
                record.getCreatedAt() != null ? record.getCreatedAt().toString() : null,
                record.getCompletedAt() != null ? record.getCompletedAt().toString() : null,
                record.getChiefComplaint(), record.getChiefComplaint(), record.getClinicalFindings(),
                clinicalResultFrom(record), record.getDiagnosis(), diagnosesFrom(record), record.getConclusion(),
                record.getConclusion(), record.getPatientInstruction(), record.getFollowUpNote(),
                prescriptionTextFrom(record), prescriptionItems
        );
    }

    private static TestResponse testFrom(TestRequest request) {
        TestResult result = request.getTestResult();
        String performedBy = result != null && result.getPerformedBy() != null && result.getPerformedBy().getProfile() != null
                ? result.getPerformedBy().getProfile().getFullName() : null;
        String collectedBy = result != null && result.getCollectedBy() != null && result.getCollectedBy().getProfile() != null
                ? result.getCollectedBy().getProfile().getFullName() : null;
        return new TestResponse(
                request.getTestRequestId().toString(), request.getTestRequestId().toString(),
                request.getService() != null ? request.getService().getName() : "Dịch vụ cận lâm sàng",
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getPerformingDepartment() != null ? request.getPerformingDepartment().getName() : null,
                request.getCreatedAt() != null ? request.getCreatedAt().toString() : null,
                false, List.of(), result != null ? result.getConclusion() : null,
                result != null && result.getImageUrl() != null
                        ? "/api/v1/test-results/" + result.getResultId() + "/file" : null, performedBy,
                result != null && result.getPerformedBy() != null ? result.getPerformedBy().getStaffId() : null,
                result != null && result.getPerformedAt() != null ? result.getPerformedAt().toString() : null,
                result != null ? result.getSampleId() : null,
                result != null && result.getSampleType() != null ? result.getSampleType().name() : null,
                result != null && result.getSampleStatus() != null ? result.getSampleStatus().name() : null,
                result != null && result.getCollectedAt() != null ? result.getCollectedAt().toString() : null,
                collectedBy
        );
    }

    private static List<DiagnosisResponse> diagnosesFrom(MedicalRecord record) {
        return record.getIcdSelections() == null ? List.of()
                : record.getIcdSelections().stream().map(DiagnosisResponse::from).toList();
    }

    private static String clinicalResultFrom(MedicalRecord record) {
        if (record.getClinicalFindings() != null && !record.getClinicalFindings().isBlank()) {
            return record.getClinicalFindings();
        }
        VitalSigns vitalSigns = record.getVitalSigns();
        if (vitalSigns == null) return null;
        StringBuilder values = new StringBuilder();
        if (vitalSigns.getBloodPressure() != null) values.append("Huyết áp: ").append(vitalSigns.getBloodPressure()).append(" ");
        if (vitalSigns.getHeartRate() != null) values.append("Nhịp tim: ").append(vitalSigns.getHeartRate()).append(" ");
        if (vitalSigns.getTemperature() != null) values.append("Nhiệt độ: ").append(vitalSigns.getTemperature()).append("°C ");
        if (vitalSigns.getWeight() != null) values.append("Cân nặng: ").append(vitalSigns.getWeight()).append("kg ");
        return values.length() == 0 ? null : values.toString().trim();
    }

    private static String prescriptionTextFrom(MedicalRecord record) {
        if (record.getPrescriptionNote() != null && !record.getPrescriptionNote().isBlank()) return record.getPrescriptionNote();
        if (record.getPrescriptionItems() == null || record.getPrescriptionItems().isEmpty()) return null;
        return record.getPrescriptionItems().stream()
                .map(item -> item.getMedicineName() + " " + item.getQuantity() + " "
                        + (item.getUnit() != null ? item.getUnit() : ""))
                .reduce((left, right) -> left + "; " + right).orElse(null);
    }

    private static String doctorName(MedicalRecord record) {
        return record.getDoctor() != null && record.getDoctor().getProfile() != null
                ? record.getDoctor().getProfile().getFullName() : null;
    }
}
