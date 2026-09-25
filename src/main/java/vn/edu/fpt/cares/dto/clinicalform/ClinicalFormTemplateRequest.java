package vn.edu.fpt.cares.dto.clinicalform;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.cares.enums.ClinicalFormContext;

import java.time.LocalDate;

public record ClinicalFormTemplateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 200) String name,
        @NotNull ClinicalFormContext context,
        @Size(max = 1000) String description,
        @NotNull JsonNode schemaJson,
        @NotBlank @Size(max = 1000) String changeReason,
        LocalDate effectiveFrom
) {}
