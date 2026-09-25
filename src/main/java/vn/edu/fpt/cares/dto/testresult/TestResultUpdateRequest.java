package vn.edu.fpt.cares.dto.testresult;

import tools.jackson.databind.JsonNode;
import vn.edu.fpt.cares.enums.SpecimenStatus;
import vn.edu.fpt.cares.enums.SpecimenType;

import java.util.UUID;

public record TestResultUpdateRequest(
        String imageUrl,
        String conclusion,
        String sampleId,
        SpecimenType sampleType,
        SpecimenStatus sampleStatus,
        JsonNode resultData,
        Boolean complete
) {
    public TestResultUpdateRequest(String imageUrl, String conclusion, String sampleId,
                                   SpecimenType sampleType, SpecimenStatus sampleStatus, Boolean complete) {
        this(imageUrl, conclusion, sampleId, sampleType, sampleStatus, null, complete);
    }
}
