package org.example.doansummer2026.dto.medicalHistory;

import java.util.List;
import java.util.UUID;

/**
 * DTO cho ket qua xet nghiem.
 */
public record TestResponse(
        String id,
        String testRequestId,
        String name,
        String status,
        String departmentName,
        String createdAt,
        boolean hasAbnormal,
        List<TestResultResponse> results,
        String conclusion,
        String pdfUrl,
        List<org.example.doansummer2026.dto.testResult.TestResultAttachmentResponse> attachments,
        String performedBy,
        UUID performedById,
        String performedAt,
        String sampleId,
        String sampleType,
        String sampleStatus,
        String collectedAt,
        String collectedBy,
        UUID orderingRecordId,
        String orderingRecordCode,
        String orderingServiceName
) {
    public record TestResultResponse(
            String name,
            String result,
            String referenceRange,
            String unit,
            String assessment
    ) {}
}
