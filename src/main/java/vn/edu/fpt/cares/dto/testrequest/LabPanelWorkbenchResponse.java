package vn.edu.fpt.cares.dto.testrequest;

import tools.jackson.databind.JsonNode;
import vn.edu.fpt.cares.dto.clinicalform.ResolvedClinicalFormResponse;
import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.TestRequestStatus;

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
