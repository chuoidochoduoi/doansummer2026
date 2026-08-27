package org.example.doansummer2026.dto.clinicalForm;

import tools.jackson.databind.JsonNode;
import org.example.doansummer2026.enums.ClinicalFormContext;

import java.util.UUID;

public record ResolvedClinicalFormResponse(
        UUID templateId, UUID templateVersionId, Integer versionNo, String code, String name,
        ClinicalFormContext context, JsonNode schema, JsonNode values
) {}
