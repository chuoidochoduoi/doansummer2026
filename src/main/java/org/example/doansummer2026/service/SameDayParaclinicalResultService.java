package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.medicalhistory.SameDayParaclinicalResultResponse;
import org.example.doansummer2026.dto.medicalhistory.TestResponse;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
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
        if (result == null || result.getVerifiedAt() == null) return java.util.Optional.empty();

        MedicalRecord sourceRecord = request.getMedicalRecord();
        CustomerVisit sourceVisit = sourceRecord.getVisit();
        var sourceQueue = sourceRecord.getQueueTicket();
        var sourceDoctor = sourceRecord.getDoctor();
        var verifier = result.getVerifiedBy();

        return java.util.Optional.of(new SameDayParaclinicalResultResponse(
                request.getTestRequestId(), result.getResultId(), request.getService().getServiceId(),
                request.getService().getServiceCode(), request.getService().getName(),
                request.getService().getDepartmentType(),
                request.getPerformingDepartment() == null ? null : request.getPerformingDepartment().getDepartmentId(),
                request.getPerformingDepartment() == null ? null : request.getPerformingDepartment().getName(),
                result.getResultData(), structuredResults(result), result.getConclusion(),
                result.getImageUrl() == null || result.getImageUrl().isBlank()
                        ? null : "/api/v1/test-results/" + result.getResultId() + "/file",
                result.getVerifiedAt(), verifier == null ? null : verifier.getStaffId(),
                verifier == null || verifier.getProfile() == null ? null : verifier.getProfile().getFullName(),
                sourceVisit.getVisitId(), visitCode(sourceVisit.getVisitId()), sourceRecord.getRecordId(),
                sourceRecord.getRecordCode(),
                sourceQueue == null || sourceQueue.getService() == null ? null : sourceQueue.getService().getName(),
                sourceDoctor == null ? null : sourceDoctor.getStaffId(),
                sourceDoctor == null || sourceDoctor.getProfile() == null ? null : sourceDoctor.getProfile().getFullName(),
                true));
    }

    private List<TestResponse.TestResultResponse> structuredResults(TestResult result) {
        if (result == null || result.getResultData() == null || result.getTestRequest() == null) {
            return java.util.List.of();
        }
        String serviceCode = result.getTestRequest().getService() != null ? 
                result.getTestRequest().getService().getServiceCode() : null;
        var panelOpt = org.example.doansummer2026.service.LaboratoryAnalyteCatalog.panel(serviceCode)
                .or(() -> org.example.doansummer2026.service.LaboratoryAnalyteCatalog.parentPanel(serviceCode));
        if (panelOpt.isEmpty()) {
            return java.util.List.of();
        }
        tools.jackson.databind.JsonNode data = result.getResultData();
        List<TestResponse.TestResultResponse> list = new java.util.ArrayList<>();
        for (var analyte : panelOpt.get().analytes()) {
            tools.jackson.databind.JsonNode valueNode = data.get(analyte.fieldKey());
            if (valueNode != null && !valueNode.isNull()) {
                list.add(new TestResponse.TestResultResponse(
                        analyte.name(),
                        valueNode.asText(),
                        "-",
                        "",
                        "NORMAL"
                ));
            }
        }
        return list;
    }

    private String visitCode(UUID visitId) {
        return "VIS-" + visitId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }
}
