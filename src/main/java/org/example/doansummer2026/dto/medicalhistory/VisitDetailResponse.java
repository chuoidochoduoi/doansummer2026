package org.example.doansummer2026.dto.medicalhistory;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.PrescriptionItem;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.model.QueueTicket;

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
        org.example.doansummer2026.dto.medicalRecord.PatientAllergyResponse patientAllergies,
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
        List<ExaminationResponse> examinations,
        List<SameDayParaclinicalResultResponse> sameDayReferencedResults,
        String completionStatus,
        List<SkippedServiceResponse> skippedServices,
        int completedExaminationCount,
        int signedTestCount
) {
    public record PrescriptionItemResponse(String medicineName, Integer quantity, String unit,
                                           String note, Integer frequencyPerDay) {
        private static PrescriptionItemResponse from(PrescriptionItem item) {
            return new PrescriptionItemResponse(item.getMedicineName(), item.getQuantity(), item.getUnit(),
                    item.getNote(), item.getFrequencyPerDay());
        }
    }

    public record ExaminationResponse(UUID recordId, String recordCode, String serviceName, UUID doctorId,
                                      String doctorName, String departmentName, String roomCode,
                                      String status, String startedAt, String completedAt,
                                      String chiefComplaint, String symptoms, String clinicalFindings,
                                      String clinicalResult, String diagnosis, List<DiagnosisResponse> diagnoses,
                                      String conclusion, String treatmentPlan, String patientInstruction,
                                      String followUpNote, String prescription,
                                      List<PrescriptionItemResponse> prescriptionItems,
                                      org.example.doansummer2026.dto.vitalsigns.VitalSignsResponse vitalSigns,
                                      org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse clinicalForm) {}

    public record SkippedServiceResponse(UUID serviceId, String serviceName, String departmentName,
                                         String roomCode, String workDate, String reason) {}

    public static VisitDetailResponse from(List<MedicalRecord> records, List<TestRequest> testRequests,
            java.util.Map<UUID, List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>> attachments) {
        return from(records, testRequests, attachments, List.of(), List.of(), false);
    }

    public static VisitDetailResponse from(List<MedicalRecord> records, List<TestRequest> testRequests,
            java.util.Map<UUID, List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>> attachments,
            List<SameDayParaclinicalResultResponse> sameDayReferencedResults) {
        return from(records, testRequests, attachments, sameDayReferencedResults, List.of(), false);
    }

    public static VisitDetailResponse publishedHistory(List<MedicalRecord> records, List<TestRequest> testRequests,
            java.util.Map<UUID, List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>> attachments,
            List<SameDayParaclinicalResultResponse> sameDayReferencedResults,
            List<QueueTicket> visitQueues) {
        return from(records, testRequests, attachments, sameDayReferencedResults, visitQueues, true);
    }

    private static VisitDetailResponse from(List<MedicalRecord> records, List<TestRequest> testRequests,
            java.util.Map<UUID, List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>> attachments,
            List<SameDayParaclinicalResultResponse> sameDayReferencedResults,
            List<QueueTicket> visitQueues, boolean publishedOnly) {
        if (records == null || records.isEmpty()) return null;

        // Ho so tam cua CLS co the duoc tao truoc benh an kham. Khong duoc
        // dung ho so tam lam noi dung chinh cua trang chi tiet.
        MedicalRecord first = records.stream()
                .filter(r -> r.getQueueTicket() != null
                        && r.getQueueTicket().getDepartment() != null
                        && r.getQueueTicket().getDepartment().getDepartmentType() == DepartmentType.EXAMINATION)
                .findFirst()
                .orElse(records.get(0));
        MedicalRecord publishedRecord = records.stream()
                .filter(r -> r.getStatus() == MedicalRecordStatus.COMPLETED)
                .filter(r -> r.getQueueTicket() != null
                        && r.getQueueTicket().getDepartment() != null
                        && r.getQueueTicket().getDepartment().getDepartmentType() == DepartmentType.EXAMINATION)
                .findFirst().orElse(null);
        MedicalRecord clinicalSource = publishedOnly ? publishedRecord : first;
        MedicalRecord feedbackRecord = records.stream()
                .filter(r -> !publishedOnly || r.getStatus() == MedicalRecordStatus.COMPLETED)
                .filter(r -> r.getRatingScore() != null || r.getFeedbackStatus() != null)
                .findFirst()
                .orElse(publishedRecord != null ? publishedRecord : first);

        List<ExaminationResponse> examinations = records.stream()
                // Standalone record chi la noi luu TestRequest; khong phai mot lan kham.
                .filter(r -> r.getQueueTicket() != null
                        && r.getQueueTicket().getDepartment() != null
                        && r.getQueueTicket().getDepartment().getDepartmentType() == DepartmentType.EXAMINATION)
                .filter(r -> !publishedOnly || r.getStatus() == MedicalRecordStatus.COMPLETED)
                .map(VisitDetailResponse::examinationFrom)
                .toList();

        List<TestResponse> tests = testRequests == null ? List.of()
                : testRequests.stream().map(request -> testFrom(request, attachments)).toList();
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
        String visitStatus = first.getVisit() != null && first.getVisit().getStatus() != null
                ? first.getVisit().getStatus().name()
                : completed ? "COMPLETED" : "IN_PROGRESS";
        String appointmentDate = first.getVisit() != null && first.getVisit().getCheckInTime() != null
                ? first.getVisit().getCheckInTime().toLocalDate().toString() : null;
        String checkInTime = first.getVisit() != null && first.getVisit().getCheckInTime() != null
                ? first.getVisit().getCheckInTime().toString() : null;
        UUID visitId = first.getVisit() != null ? first.getVisit().getVisitId() : null;
        List<SkippedServiceResponse> skippedServices = visitQueues == null ? List.of() : visitQueues.stream()
                .filter(queue -> queue.getStatus() == org.example.doansummer2026.enums.QueueStatus.SKIPPED)
                .map(queue -> new SkippedServiceResponse(
                        queue.getService() != null ? queue.getService().getServiceId() : null,
                        queue.getService() != null ? queue.getService().getName() : "Dịch vụ khám",
                        queue.getDepartment() != null ? queue.getDepartment().getName() : null,
                        queue.getDepartment() != null ? queue.getDepartment().getRoomCode() : null,
                        queue.getWorkDate() != null ? queue.getWorkDate().toString() : null,
                        "Đã bỏ lượt"))
                .toList();
        String completionStatus = skippedServices.isEmpty() ? "COMPLETE" : "PARTIAL";

        return new VisitDetailResponse(
                first.getRecordId(),
                visitId,
                visitId != null
                        ? "VIS-" + visitId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT)
                        : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null ? first.getVisit().getCustomer().getFullName() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null && first.getVisit().getCustomer().getDateOfBirth() != null ? first.getVisit().getCustomer().getDateOfBirth().toString() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null && first.getVisit().getCustomer().getGender() != null ? first.getVisit().getCustomer().getGender().name() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null ? first.getVisit().getCustomer().getPhone() : null,
                first.getVisit() != null && first.getVisit().getCustomer() != null ? first.getVisit().getCustomer().getAddress() : null,
                org.example.doansummer2026.dto.medicalRecord.PatientAllergyResponse.from(
                        first.getVisit() == null ? null : first.getVisit().getCustomer()),
                first.getRecordCode(),
                appointmentDate,
                checkInTime,
                clinicalSource != null ? clinicalSource.getChiefComplaint() : null,
                clinicalSource != null ? clinicalResultFrom(clinicalSource) : null,
                clinicalSource != null ? diagnosesFrom(clinicalSource) : List.of(),
                clinicalSource != null ? clinicalSource.getConclusion() : null,
                clinicalSource != null ? clinicalSource.getPatientInstruction() : null,
                clinicalSource != null ? prescriptionTextFrom(clinicalSource) : null,
                tests,
                visitStatus,
                feedbackRecord.getRatingScore(),
                feedbackRecord.getRatingComment(),
                feedbackRecord.getRatedAt() != null ? feedbackRecord.getRatedAt().toString() : null,
                feedbackRecord.getFeedbackStatus(),
                feedbackRecord.getManagerResponse(),
                feedbackRecord.getRespondedAt() != null ? feedbackRecord.getRespondedAt().toString() : null,
                feedbackRecord.getRespondedBy() != null && feedbackRecord.getRespondedBy().getProfile() != null
                        ? feedbackRecord.getRespondedBy().getProfile().getFullName() : null,
                clinicalSource != null ? doctorName(clinicalSource) : null,
                labDoctors,
                examinations,
                sameDayReferencedResults == null ? List.of() : sameDayReferencedResults,
                completionStatus, skippedServices, examinations.size(), tests.size()
        );
    }

    private static ExaminationResponse examinationFrom(MedicalRecord record) {
        List<PrescriptionItemResponse> prescriptionItems = record.getPrescriptionItems() == null ? List.of()
                : record.getPrescriptionItems().stream().map(PrescriptionItemResponse::from).toList();
        return new ExaminationResponse(
                record.getRecordId(), record.getRecordCode(),
                record.getQueueTicket().getService() != null ? record.getQueueTicket().getService().getName() : "Khám bệnh",
                record.getDoctor() != null ? record.getDoctor().getStaffId() : null,
                doctorName(record),
                record.getQueueTicket().getDepartment() != null
                        ? record.getQueueTicket().getDepartment().getName() : null,
                record.getQueueTicket().getDepartment() != null
                        ? record.getQueueTicket().getDepartment().getRoomCode() : null,
                record.getStatus() != null ? record.getStatus().name() : null,
                record.getCreatedAt() != null ? record.getCreatedAt().toString() : null,
                record.getCompletedAt() != null ? record.getCompletedAt().toString() : null,
                record.getChiefComplaint(), record.getChiefComplaint(), record.getClinicalFindings(),
                clinicalResultFrom(record), record.getDiagnosis(), diagnosesFrom(record), record.getConclusion(),
                record.getConclusion(), record.getPatientInstruction(), record.getFollowUpNote(),
                prescriptionTextFrom(record), prescriptionItems,
                record.getVitalSigns() != null
                        ? org.example.doansummer2026.dto.vitalsigns.VitalSignsResponse.from(record.getVitalSigns())
                        : null,
                clinicalFormFrom(record)
        );
    }

    private static org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse clinicalFormFrom(
            MedicalRecord record) {
        var version = record.getFormTemplateVersion();
        if (version == null || version.getTemplate() == null) return null;
        var template = version.getTemplate();
        return new org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse(
                template.getTemplateId(), version.getVersionId(), version.getVersionNo(), template.getCode(),
                template.getName(), template.getContext(), version.getSchemaJson(), record.getSpecialtyData());
    }

    private static TestResponse testFrom(TestRequest request,
            java.util.Map<UUID, List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>> attachments) {
        TestResult result = request.getTestResult();
        String serviceCode = request.getService() != null ? request.getService().getServiceCode() : null;
        var panel = org.example.doansummer2026.service.LaboratoryAnalyteCatalog.panel(serviceCode)
                .or(() -> org.example.doansummer2026.service.LaboratoryAnalyteCatalog.parentPanel(serviceCode))
                .orElse(null);
        String performedBy = result != null && result.getPerformedBy() != null && result.getPerformedBy().getProfile() != null
                ? result.getPerformedBy().getProfile().getFullName() : null;
        String collectedBy = result != null && result.getCollectedBy() != null && result.getCollectedBy().getProfile() != null
                ? result.getCollectedBy().getProfile().getFullName() : null;
        MedicalRecord orderingRecord = request.getMedicalRecord();
        String orderingServiceName = orderingRecord != null && orderingRecord.getQueueTicket() != null
                && orderingRecord.getQueueTicket().getService() != null
                ? orderingRecord.getQueueTicket().getService().getName() : null;
        return new TestResponse(
                request.getTestRequestId().toString(), request.getTestRequestId().toString(),
                request.getService() != null ? request.getService().getName() : "Dịch vụ cận lâm sàng",
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getPerformingDepartment() != null ? request.getPerformingDepartment().getName() : null,
                request.getCreatedAt() != null ? request.getCreatedAt().toString() : null,
                hasAbnormal(result), structuredResults(result), result != null ? result.getConclusion() : null,
                result != null && result.getImageUrl() != null
                        ? "/api/v1/test-results/" + result.getResultId() + "/file" : null,
                result == null ? List.of() : attachments.getOrDefault(result.getResultId(), List.of()), performedBy,
                result != null && result.getPerformedBy() != null ? result.getPerformedBy().getStaffId() : null,
                result != null && result.getPerformedAt() != null ? result.getPerformedAt().toString() : null,
                result != null ? result.getSampleId() : null,
                result != null && result.getSampleType() != null ? result.getSampleType().name() : null,
                result != null && result.getSampleStatus() != null ? result.getSampleStatus().name() : null,
                result != null && result.getCollectedAt() != null ? result.getCollectedAt().toString() : null,
                collectedBy,
                orderingRecord != null && orderingRecord.getQueueTicket() != null
                        ? orderingRecord.getRecordId() : null,
                orderingRecord != null && orderingRecord.getQueueTicket() != null
                        ? orderingRecord.getRecordCode() : null,
                orderingServiceName,
                serviceCode,
                panel != null ? panel.serviceCode() : null,
                panel != null ? panel.name() : null,
                panel != null ? panel.analytes().size() : null,
                request.getQueueTicket() != null ? request.getQueueTicket().getTicketId() : null
        );
    }

    private static List<TestResponse.TestResultResponse> structuredResults(TestResult result) {
        if (result == null || result.getResultData() == null || result.getFormTemplateVersion() == null
                || result.getFormTemplateVersion().getSchemaJson() == null) return List.of();
        var schema = result.getFormTemplateVersion().getSchemaJson();
        java.util.List<tools.jackson.databind.JsonNode> fields = new java.util.ArrayList<>();
        if (schema.path("fields").isArray()) schema.path("fields").forEach(fields::add);
        if (schema.path("sections").isArray()) schema.path("sections").forEach(section -> {
            if (section.path("fields").isArray()) section.path("fields").forEach(fields::add);
        });
        var flags = result.getResultData().path("_meta").path("flags");
        return fields.stream().filter(field -> result.getResultData().hasNonNull(field.path("key").asText()))
                .map(field -> {
                    String key = field.path("key").asText();
                    var flag = flags.path(key);
                    var range = flag.path("referenceRange");
                    String rangeText = range.isObject()
                            ? (range.has("low") ? range.path("low").asText() : "") + " - "
                            + (range.has("high") ? range.path("high").asText() : "") : null;
                    return new TestResponse.TestResultResponse(field.path("label").asText(key),
                            result.getResultData().path(key).asText(), rangeText,
                            field.path("unit").asText(null), flag.path("status").asText("NOT_EVALUATED"));
                }).toList();
    }

    private static boolean hasAbnormal(TestResult result) {
        return structuredResults(result).stream().anyMatch(item ->
                java.util.Set.of("LOW", "HIGH", "ABNORMAL").contains(item.assessment()));
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
