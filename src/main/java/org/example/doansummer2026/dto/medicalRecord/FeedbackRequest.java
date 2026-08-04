package org.example.doansummer2026.dto.medicalRecord;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @Min(1) @Max(5) int overallRating,
        @Size(max = 500) String comment
) {}
