package org.example.doansummer2026.dto.testResult;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import org.example.doansummer2026.enums.SpecimenStatus;
import org.example.doansummer2026.enums.SpecimenType;

public record TestResultCreateRequest(
        @NotNull UUID testRequestId,
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        @NotNull UUID performedById
) {}



