package org.example.doansummer2026.dto.medicalRecord;

import java.time.LocalDate;

public record FollowUpRequest(
        String note,
        LocalDate preferredDate
) {}
