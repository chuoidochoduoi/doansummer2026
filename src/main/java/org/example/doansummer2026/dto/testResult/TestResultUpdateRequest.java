package org.example.doansummer2026.dto.testResult;

import org.example.doansummer2026.enums.SpecimenStatus;
import org.example.doansummer2026.enums.SpecimenType;

public record TestResultUpdateRequest(
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        Boolean complete
) {}



