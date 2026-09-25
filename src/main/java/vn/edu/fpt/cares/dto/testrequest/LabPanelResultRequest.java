package vn.edu.fpt.cares.dto.testrequest;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.cares.enums.SpecimenStatus;
import vn.edu.fpt.cares.enums.SpecimenType;

import java.util.UUID;

/** Payload for saving the selected analytes of one panel. */
public record LabPanelResultRequest(
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        JsonNode resultData,
        @NotNull UUID performedById
) {}
