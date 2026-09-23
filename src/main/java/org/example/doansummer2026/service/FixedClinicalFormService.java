package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse;
import org.example.doansummer2026.enums.ClinicalFormContext;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads the fixed, system-owned result form configured for a paraclinical service. */
@Service
public class FixedClinicalFormService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FixedClinicalFormService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public ResolvedClinicalFormResponse resolveForService(UUID serviceId, JsonNode values) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT t.template_id, v.version_id, v.version_no, t.code, t.name, t.context,
                       v.schema_json::text AS schema_json
                FROM medical_service_form_template b
                JOIN clinical_form_template t ON t.template_id = b.template_id
                JOIN clinical_form_template_version v ON v.template_id = t.template_id
                WHERE b.service_id = ?
                  AND b.deleted = false AND t.deleted = false AND t.active = true
                  AND v.deleted = false AND v.status = 'PUBLISHED'
                  AND (v.effective_from IS NULL OR v.effective_from <= CURRENT_DATE)
                ORDER BY v.version_no DESC
                LIMIT 1
                """, serviceId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Dịch vụ chưa được cấu hình biểu mẫu kết quả");
        }

        Map<String, Object> row = rows.get(0);
        try {
            return new ResolvedClinicalFormResponse(
                    UUID.fromString(row.get("template_id").toString()),
                    UUID.fromString(row.get("version_id").toString()),
                    ((Number) row.get("version_no")).intValue(),
                    row.get("code").toString(), row.get("name").toString(),
                    ClinicalFormContext.valueOf(row.get("context").toString()),
                    objectMapper.readTree(row.get("schema_json").toString()), values);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể đọc cấu hình biểu mẫu kết quả", ex);
        }
    }
}
