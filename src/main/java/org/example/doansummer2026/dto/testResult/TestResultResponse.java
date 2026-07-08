package org.example.doansummer2026.dto.testResult;

import org.example.doansummer2026.model.TestResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestResultResponse(
        UUID resultId,
        UUID testRequestId,
        String imageUrl,
        String conclusion,
        UUID performedById,
        String performedByName,
        LocalDateTime performedAt
) {
    public static TestResultResponse from(TestResult r) {
        UUID reqId = r.getTestRequest() != null ? r.getTestRequest().getTestRequestId() : null;
        UUID performedById = r.getPerformedBy() != null ? r.getPerformedBy().getStaffId() : null;
        String performedByName = r.getPerformedBy() != null ? r.getPerformedBy().getStaffCode() : null;
        return new TestResultResponse(r.getResultId(), reqId, r.getImageUrl(), r.getConclusion(),
                performedById, performedByName, r.getPerformedAt());
    }
}
