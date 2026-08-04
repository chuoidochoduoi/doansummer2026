package org.example.doansummer2026.dto.medicalHistory;

import java.util.List;
import java.util.UUID;

/**
 * DTO cho ket qua xet nghiem.
 */
public record TestResponse(
        String id,
        String name,
        boolean hasAbnormal,
        List<TestResultResponse> results,
        String conclusion,
        String pdfUrl,
        String performedBy,
        UUID performedById,
        String performedAt,
        String sampleId
) {
    public record TestResultResponse(
            String name,
            String result,
            String referenceRange,
            String unit,
            String assessment
    ) {}
}
