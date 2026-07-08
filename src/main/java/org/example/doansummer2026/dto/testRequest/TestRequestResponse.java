package org.example.doansummer2026.dto.testRequest;

import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.TestRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestRequestResponse(
        UUID testRequestId,
        UUID medicalRecordId,
        UUID serviceId,
        String serviceName,
        UUID performingDepartmentId,
        String performingDepartmentName,
        String description,
        TestRequestStatus status,
        UUID requestedById,
        String requestedByName,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        UUID testResultId
) {
    public static TestRequestResponse from(TestRequest t) {
        UUID recordId = t.getMedicalRecord() != null ? t.getMedicalRecord().getRecordId() : null;
        UUID serviceId = t.getService() != null ? t.getService().getServiceId() : null;
        String serviceName = t.getService() != null ? t.getService().getName() : null;
        UUID deptId = t.getPerformingDepartment() != null ? t.getPerformingDepartment().getDepartmentId() : null;
        String deptName = t.getPerformingDepartment() != null ? t.getPerformingDepartment().getName() : null;
        UUID reqById = t.getRequestedBy() != null ? t.getRequestedBy().getStaffId() : null;
        String reqByName = t.getRequestedBy() != null ? t.getRequestedBy().getStaffCode() : null;
        UUID resultId = t.getTestResult() != null ? t.getTestResult().getResultId() : null;
        return new TestRequestResponse(t.getTestRequestId(), recordId, serviceId, serviceName,
                deptId, deptName, t.getDescription(), t.getStatus(), reqById, reqByName,
                t.getCompletedAt(), t.getCreatedAt(), resultId);
    }
}
