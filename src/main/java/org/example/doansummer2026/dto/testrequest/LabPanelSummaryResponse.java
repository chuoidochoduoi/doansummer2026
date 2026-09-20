package org.example.doansummer2026.dto.testrequest;

import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Read model used by the laboratory worklist. A billable analyte remains a
 * TestRequest, but related analytes are presented as one laboratory panel. */
public record LabPanelSummaryResponse(
        UUID representativeId,
        UUID queueTicketId,
        Integer queueNumber,
        QueueStatus queueStatus,
        UUID performingDepartmentId,
        String panelCode,
        String panelName,
        String patientCode,
        String patientName,
        LocalDateTime createdAt,
        TestRequestStatus status,
        int purchasedCount,
        int totalAnalyteCount,
        int completedCount,
        boolean grouped,
        boolean requiresSpecimen,
        boolean specimenReadyForRelease,
        boolean serviceReadyForRelease
) {}
