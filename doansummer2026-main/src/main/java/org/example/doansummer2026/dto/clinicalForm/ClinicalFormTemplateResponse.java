package org.example.doansummer2026.dto.clinicalForm;

import tools.jackson.databind.JsonNode;
import org.example.doansummer2026.enums.ClinicalFormContext;
import org.example.doansummer2026.enums.ClinicalTemplateStatus;
import org.example.doansummer2026.model.ClinicalFormTemplate;
import org.example.doansummer2026.model.ClinicalFormTemplateVersion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClinicalFormTemplateResponse(
        UUID templateId, String code, String name, ClinicalFormContext context, String description,
        UUID versionId, Integer versionNo, ClinicalTemplateStatus status, JsonNode schemaJson,
        String changeReason, LocalDate effectiveFrom, LocalDateTime publishedAt,
        List<UUID> serviceIds
) {
    public static ClinicalFormTemplateResponse from(ClinicalFormTemplate t, ClinicalFormTemplateVersion v, List<UUID> serviceIds) {
        return new ClinicalFormTemplateResponse(t.getTemplateId(), t.getCode(), t.getName(), t.getContext(), t.getDescription(),
                v == null ? null : v.getVersionId(), v == null ? null : v.getVersionNo(), v == null ? null : v.getStatus(),
                v == null ? null : v.getSchemaJson(), v == null ? null : v.getChangeReason(),
                v == null ? null : v.getEffectiveFrom(), v == null ? null : v.getPublishedAt(), serviceIds);
    }
}
