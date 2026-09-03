package org.example.doansummer2026.dto.clinicalform;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClinicalFormDraftRequest(
        @NotNull JsonNode schemaJson,
        @NotBlank @Size(max = 1000) String changeReason,
        LocalDate effectiveFrom
) {}
