package vn.edu.fpt.cares.dto.clinicalform;

import tools.jackson.databind.JsonNode;
import vn.edu.fpt.cares.enums.ClinicalFormContext;

import java.util.UUID;

public record ResolvedClinicalFormResponse(
        UUID templateId, UUID templateVersionId, Integer versionNo, String code, String name,
        ClinicalFormContext context, JsonNode schema, JsonNode values
) {}
