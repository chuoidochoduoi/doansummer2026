package org.example.doansummer2026.dto.testresult;

import tools.jackson.databind.JsonNode;
import org.example.doansummer2026.enums.SpecimenStatus;
import org.example.doansummer2026.enums.SpecimenType;

import java.util.UUID;

public record TestResultUpdateRequest(
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        UUID formTemplateVersionId,
        JsonNode resultData,
        Boolean complete
) {
    public TestResultUpdateRequest(String imageUrl, String conclusion, String sampleId,
                                   SpecimenType sampleType, SpecimenStatus sampleStatus, Boolean complete) {
        this(imageUrl, conclusion, sampleId, sampleType, sampleStatus, null, null, complete);
    }
}

