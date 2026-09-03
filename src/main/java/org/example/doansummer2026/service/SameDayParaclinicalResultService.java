package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.medicalhistory.SameDayParaclinicalResultResponse;
import org.example.doansummer2026.dto.medicalhistory.TestResponse;
import org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.TestResultRevisionStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.model.TestResultRevision;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.TestResultAttachmentRepository;
import org.example.doansummer2026.repository.TestResultRevisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SameDayParaclinicalResultService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MedicalRecordRepository recordRepository;
    private final CustomerVisitRepository visitRepository;
    private final ProfileRepository profileRepository;
    private final TestRequestRepository testRequestRepository;
    private final TestResultRevisionRepository revisionRepository;
    private final TestResultAttachmentRepository attachmentRepository;

    public List<SameDayParaclinicalResultResponse> findForRecord(UUID recordId) {
        MedicalRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khám"));
        if (record.getVisit() == null) return List.of();
        return findForVisit(record.getVisit());
    }

    public List<SameDayParaclinicalResultResponse> findForVisit(UUID visitId) {
        CustomerVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        return findForVisit(visit);
    }

    public List<SameDayParaclinicalResultResponse> findForCustomerToday(UUID profileId) {
        Profile patient = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bệnh nhân"));
        return find(patient, LocalDate.now(CLINIC_ZONE), null, LocalDateTime.now(CLINIC_ZONE));
    }

    public boolean hasReusableResult(CustomerVisit targetVisit, UUID serviceId) {
        if (serviceId == null) return false;
        return findForVisit(targetVisit).stream().anyMatch(item -> serviceId.equals(item.serviceId()));
    }

    private List<SameDayParaclinicalResultResponse> findForVisit(CustomerVisit visit) {
        if (visit == null || visit.getCustomer() == null || visit.getCheckInTime() == null) return List.of();
        LocalDateTime cutoff = visit.getCheckOutTime() != null
                ? visit.getCheckOutTime() : LocalDateTime.now(CLINIC_ZONE);
        return find(visit.getCustomer(), visit.getCheckInTime().toLocalDate(), visit.getVisitId(), cutoff);
    }

    private List<SameDayParaclinicalResultResponse> find(Profile patient, LocalDate targetDate,
                                                          UUID excludedVisitId, LocalDateTime cutoff) {
        Map<UUID, SameDayParaclinicalResultResponse> latestByService = new LinkedHashMap<>();
        testRequestRepository.findByProfileIdAndStatusCompleted(patient.getProfileId()).stream()
                .filter(request -> request.getStatus() == TestRequestStatus.COMPLETED)
                .filter(request -> request.getCompletedAt() != null
                        && request.getCompletedAt().toLocalDate().equals(targetDate)
                        && !request.getCompletedAt().isAfter(cutoff))
                .filter(request -> request.getMedicalRecord() != null
                        && request.getMedicalRecord().getVisit() != null)
                .filter(request -> excludedVisitId == null
                        || !excludedVisitId.equals(request.getMedicalRecord().getVisit().getVisitId()))
                .filter(request -> request.getService() != null
                        && request.getService().getDepartmentType() != null
                        && request.getService().getDepartmentType().isParaclinical())
                .filter(request -> request.getTestResult() != null)
                .sorted(Comparator.comparing(TestRequest::getCompletedAt).reversed())
                .forEach(request -> toResponse(request)
                        .filter(response -> response.verifiedAt() != null
                                && response.verifiedAt().toLocalDate().equals(targetDate)
                                && !response.verifiedAt().isAfter(cutoff))
                        .ifPresent(response -> latestByService.putIfAbsent(
                                response.serviceId(), response)));
        return List.copyOf(latestByService.values());
    }

    private java.util.Optional<SameDayParaclinicalResultResponse> toResponse(TestRequest request) {
        var result = request.getTestResult();
        TestResultRevision signed = revisionRepository
                .findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                        result.getResultId(), TestResultRevisionStatus.SIGNED)
                .orElse(null);
        if (signed == null || signed.getSignedAt() == null) return java.util.Optional.empty();

        MedicalRecord sourceRecord = request.getMedicalRecord();
        CustomerVisit sourceVisit = sourceRecord.getVisit();
        var sourceQueue = sourceRecord.getQueueTicket();
        var sourceDoctor = sourceRecord.getDoctor();
        var verifier = signed.getSignedBy();
        List<TestResultAttachmentResponse> attachments = attachmentRepository
                .findByRevision_RevisionIdOrderByDisplayOrder(signed.getRevisionId()).stream()
                .map(TestResultAttachmentResponse::from).toList();

        return java.util.Optional.of(new SameDayParaclinicalResultResponse(
                request.getTestRequestId(), result.getResultId(), request.getService().getServiceId(),
                request.getService().getServiceCode(), request.getService().getName(),
                request.getService().getDepartmentType(),
                request.getPerformingDepartment() == null ? null : request.getPerformingDepartment().getDepartmentId(),
                request.getPerformingDepartment() == null ? null : request.getPerformingDepartment().getName(),
                signed.getResultData(), structuredResults(signed), signed.getConclusion(), attachments,
                signed.getSignedAt(), verifier == null ? null : verifier.getStaffId(),
                verifier == null || verifier.getProfile() == null ? null : verifier.getProfile().getFullName(),
                sourceVisit.getVisitId(), visitCode(sourceVisit.getVisitId()), sourceRecord.getRecordId(),
                sourceRecord.getRecordCode(),
                sourceQueue == null || sourceQueue.getService() == null ? null : sourceQueue.getService().getName(),
                sourceDoctor == null ? null : sourceDoctor.getStaffId(),
                sourceDoctor == null || sourceDoctor.getProfile() == null ? null : sourceDoctor.getProfile().getFullName(),
                true));
    }

    private List<TestResponse.TestResultResponse> structuredResults(TestResultRevision revision) {
        JsonNode data = revision.getResultData();
        var version = revision.getTemplateVersion();
        if (data == null || version == null || version.getSchemaJson() == null) return List.of();
        JsonNode schema = version.getSchemaJson();
        List<JsonNode> fields = new ArrayList<>();
        if (schema.path("fields").isArray()) schema.path("fields").forEach(fields::add);
        if (schema.path("sections").isArray()) schema.path("sections").forEach(section -> {
            if (section.path("fields").isArray()) section.path("fields").forEach(fields::add);
        });
        JsonNode flags = data.path("_meta").path("flags");
        return fields.stream().filter(field -> data.hasNonNull(field.path("key").asText()))
                .map(field -> {
                    String key = field.path("key").asText();
                    JsonNode flag = flags.path(key);
                    JsonNode range = flag.path("referenceRange");
                    String rangeText = range.isObject()
                            ? (range.has("low") ? range.path("low").asText() : "") + " - "
                            + (range.has("high") ? range.path("high").asText() : "") : null;
                    return new TestResponse.TestResultResponse(field.path("label").asText(key),
                            data.path(key).asText(), rangeText, field.path("unit").asText(null),
                            flag.path("status").asText("NOT_EVALUATED"));
                }).toList();
    }

    private String visitCode(UUID visitId) {
        return "VIS-" + visitId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }
}
