package vn.edu.fpt.cares.dto.medicalhistory;

import java.util.List;
import java.util.UUID;

/** Mot dong lich su tuong ung mot CustomerVisit, khong phai mot MedicalRecord. */
public record VisitHistorySummaryResponse(
        UUID id,
        UUID visitId,
        String visitCode,
        String date,
        String time,
        String status,
        List<String> serviceNames,
        int examinationCount,
        int testCount,
        List<String> doctorNames,
        String diagnosisSummary,
        String completionStatus,
        List<String> completedServiceNames,
        List<String> skippedServiceNames,
        int skippedServiceCount
) {}
