package vn.edu.fpt.cares.dto.medicalrecord;

import java.time.LocalDate;

public record FollowUpRequest(
        String note,
        LocalDate preferredDate
) {}
