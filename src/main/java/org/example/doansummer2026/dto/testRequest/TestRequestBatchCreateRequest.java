package org.example.doansummer2026.dto.testRequest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Tao nhieu TestRequest cung luc tu medical record.
 * performingDepartmentId la optional - neu null thi lay tu tung MedicalService.
 */
public record TestRequestBatchCreateRequest(
        @NotNull UUID medicalRecordId,
        UUID performingDepartmentId,
        @NotNull UUID requestedById,
        @NotEmpty List<UUID> serviceIds,
        String notes
) {}



