package org.example.doansummer2026.dto.testResult;

import org.example.doansummer2026.model.TestResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestResultResponse(
        UUID resultId,
        UUID testRequestId,
        String imageUrl,
        String fileName,
        String conclusion,
        String sampleId,
        UUID performedById,
        String performedByName,
        LocalDateTime performedAt
) {
    public static TestResultResponse from(TestResult r) {
        UUID reqId = r.getTestRequest() != null ? r.getTestRequest().getTestRequestId() : null;
        UUID performedById = r.getPerformedBy() != null ? r.getPerformedBy().getStaffId() : null;
        String performedByName = r.getPerformedBy() != null ? r.getPerformedBy().getStaffCode() : null;
        String fileName = r.getImageUrl() != null ? extractFileName(r.getImageUrl()) : null;
        return new TestResultResponse(r.getResultId(), reqId, r.getImageUrl(), fileName,
                r.getConclusion(), r.getSampleId(), performedById, performedByName, r.getPerformedAt());
    }

    private static String extractFileName(String imageUrl) {
        if (imageUrl == null) return null;
        int idx = imageUrl.lastIndexOf('/');
        return idx >= 0 ? imageUrl.substring(idx + 1) : imageUrl;
    }
}




