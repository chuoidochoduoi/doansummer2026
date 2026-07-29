package org.example.doansummer2026.dto.testRequest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Tao TestRequest - performingDepartment tu dong lay tu MedicalService.department.
 * invoiceItemId: lien ket voi InvoiceItem tu hoa don (de trace luong Invoice -> TestRequest).
 */
public record TestRequestCreateRequest(
        @NotNull UUID medicalRecordId,
        @NotNull UUID serviceId,
        @NotNull UUID requestedById,
        String notes,
        UUID invoiceItemId
) {}




