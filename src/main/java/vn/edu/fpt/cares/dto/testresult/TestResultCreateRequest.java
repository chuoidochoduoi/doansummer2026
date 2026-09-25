package vn.edu.fpt.cares.dto.testresult;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import vn.edu.fpt.cares.enums.SpecimenStatus;
import vn.edu.fpt.cares.enums.SpecimenType;

public record TestResultCreateRequest(
        @NotNull UUID testRequestId,
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        JsonNode resultData,
        @NotNull UUID performedById
) {
    public TestResultCreateRequest(UUID testRequestId, String imageUrl, String conclusion, String sampleId,
                                   SpecimenType sampleType, SpecimenStatus sampleStatus, UUID performedById) {
        this(testRequestId, imageUrl, conclusion, sampleId, sampleType, sampleStatus, null, performedById);
    }
}
