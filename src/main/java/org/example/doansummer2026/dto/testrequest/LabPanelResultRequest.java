package org.example.doansummer2026.dto.testrequest;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import org.example.doansummer2026.enums.SpecimenStatus;
import org.example.doansummer2026.enums.SpecimenType;

import java.util.UUID;

/** Payload for saving the selected analytes of one panel. */
public record LabPanelResultRequest(
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        UUID formTemplateVersionId,
        JsonNode resultData,
        @NotNull UUID performedById
) {}
