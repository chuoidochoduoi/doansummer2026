package org.example.doansummer2026.dto.testResult;

import tools.jackson.databind.JsonNode;
import org.example.doansummer2026.enums.TestResultRevisionStatus;
import org.example.doansummer2026.model.TestResultRevision;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestResultRevisionResponse(
        UUID revisionId, Integer revisionNo, TestResultRevisionStatus status, JsonNode resultData,
        String conclusion, UUID templateVersionId, String amendmentReason,
        UUID signedById, String signedByName, LocalDateTime signedAt
) {
    public static TestResultRevisionResponse from(TestResultRevision r) {
        return new TestResultRevisionResponse(r.getRevisionId(), r.getRevisionNo(), r.getStatus(), r.getResultData(),
                r.getConclusion(), r.getTemplateVersion() == null ? null : r.getTemplateVersion().getVersionId(),
                r.getAmendmentReason(), r.getSignedBy() == null ? null : r.getSignedBy().getStaffId(),
                r.getSignedBy() == null || r.getSignedBy().getProfile() == null ? null : r.getSignedBy().getProfile().getFullName(),
                r.getSignedAt());
    }
}
