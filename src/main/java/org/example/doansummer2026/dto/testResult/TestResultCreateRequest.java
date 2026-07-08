package org.example.doansummer2026.dto.testResult;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TestResultCreateRequest(
        @NotNull UUID testRequestId,
        String imageUrl,
        String conclusion,
        @NotNull UUID performedById
) {}
