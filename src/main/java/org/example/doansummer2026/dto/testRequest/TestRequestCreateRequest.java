package org.example.doansummer2026.dto.testRequest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TestRequestCreateRequest(
        @NotNull UUID medicalRecordId,
        @NotNull UUID serviceId,
        @NotNull UUID performingDepartmentId,
        @NotNull UUID requestedById,
        String description
) {}
