package org.example.doansummer2026.dto.medicalHistory;

import java.util.List;

/**
 * DTO cho ket qua xet nghiem.
 */
public record TestResponse(
        String id,
        String name,
        boolean hasAbnormal,
        List<TestResultResponse> results
) {
    public record TestResultResponse(
            String name,
            String result,
            String referenceRange,
            String unit,
            String assessment
    ) {}
}