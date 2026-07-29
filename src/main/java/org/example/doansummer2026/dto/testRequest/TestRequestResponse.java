package org.example.doansummer2026.dto.testRequest;

import org.example.doansummer2026.model.TestRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.DepartmentType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response cho danh sach hang choi TestRequest.
 * Bao gom: Ma yeu cau, Ma benh nhan, Ho ten, Thoi gian, Loai xet nghiem, Trang thai.
 */
public record TestRequestResponse(
        UUID testRequestId,
        UUID medicalRecordId,
        UUID serviceId,
        String serviceName,
        DepartmentType serviceType,
        UUID performingDepartmentId,
        String performingDepartmentName,
        String description,
        TestRequestStatus status,
        UUID requestedById,
        String requestedByName,
        LocalDateTime completedAt,
        String cancelReason,
        LocalDateTime createdAt,
        UUID testResultId,
        UUID invoiceItemId,
        // Thong tin benh nhan cho danh sach
        String patientCode,
        String patientName
) {
    public static TestRequestResponse from(TestRequest t) {
        UUID recordId = t.getMedicalRecord() != null ? t.getMedicalRecord().getRecordId() : null;
        UUID serviceId = t.getService() != null ? t.getService().getServiceId() : null;
        String serviceName = t.getService() != null ? t.getService().getName() : null;
        DepartmentType serviceType = t.getService() != null ? t.getService().getDepartmentType() : null;
        UUID deptId = t.getPerformingDepartment() != null ? t.getPerformingDepartment().getDepartmentId() : null;
        String deptName = t.getPerformingDepartment() != null ? t.getPerformingDepartment().getName() : null;
        UUID reqById = t.getRequestedBy() != null ? t.getRequestedBy().getStaffId() : null;
        String reqByName = t.getRequestedBy() != null ? t.getRequestedBy().getStaffCode() : null;
        UUID resultId = t.getTestResult() != null ? t.getTestResult().getResultId() : null;
        UUID invoiceItemId = t.getInvoiceItem() != null ? t.getInvoiceItem().getItemId() : null;

        // Lay thong tin benh nhan
        String patientCode = null;
        String patientName = null;
        if (t.getMedicalRecord() != null && t.getMedicalRecord().getVisit() != null) {
            var visit = t.getMedicalRecord().getVisit();
            if (visit.getCustomer() != null) {
                patientCode = visit.getCustomer().getPhone(); // Phone lam ma benh nhan
                patientName = visit.getCustomer().getFullName();
            }
        }

        return new TestRequestResponse(t.getTestRequestId(), recordId, serviceId, serviceName,
                serviceType, deptId, deptName, t.getDescription(), t.getStatus(), reqById, reqByName,
                t.getCompletedAt(), t.getCancelReason(), t.getCreatedAt(), resultId, invoiceItemId,
                patientCode, patientName);
    }
}




