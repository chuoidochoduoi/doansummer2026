package org.example.doansummer2026.dto.medicalHistory;

import org.example.doansummer2026.dto.testResult.TestResultAttachmentResponse;
import org.example.doansummer2026.enums.DepartmentType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Ket qua CLS cua mot luot khac, chi duoc tham chieu va khong doi luot so huu. */
public record SameDayParaclinicalResultResponse(
        UUID testRequestId,
        UUID resultId,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        DepartmentType departmentType,
        UUID performingDepartmentId,
        String performingDepartmentName,
        JsonNode resultData,
        List<TestResponse.TestResultResponse> results,
        String conclusion,
        List<TestResultAttachmentResponse> attachments,
        LocalDateTime verifiedAt,
        UUID verifiedById,
        String verifiedByName,
        UUID sourceVisitId,
        String sourceVisitCode,
        UUID sourceRecordId,
        String sourceRecordCode,
        String sourceExaminationServiceName,
        UUID sourceDoctorId,
        String sourceDoctorName,
        boolean readOnly
) {}
