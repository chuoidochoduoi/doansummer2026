package org.example.doansummer2026.dto.testrequest;

import tools.jackson.databind.JsonNode;
import org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.TestRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LabPanelWorkbenchResponse(
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
        String sampleId,
        String sampleType,
        String sampleStatus,
        String conclusion,
        JsonNode resultData,
        UUID formTemplateVersionId,
        ResolvedClinicalFormResponse clinicalForm,
        int purchasedCount,
        int totalAnalyteCount,
        int completedCount,
        List<AnalyteItem> analytes
) {
    public record AnalyteItem(
            String serviceCode,
            String fieldKey,
            String name,
            boolean purchased,
            UUID testRequestId,
            TestRequestStatus status
    ) {}
}
