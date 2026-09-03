package org.example.doansummer2026.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LaboratoryAnalyteCatalogTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void chiSoLeChiNhanDungTruongDaThanhToan() throws Exception {
        var schema = mapper.readTree("""
                {"fields":[
                  {"key":"hba1c","label":"HbA1c","requiredOnSign":true},
                  {"key":"totalCholesterol","label":"Cholesterol","requiredOnSign":true}
                ],"rules":[{"type":"SUM_BETWEEN"}]}
                """);

        var filtered = LaboratoryAnalyteCatalog.schemaForService("AN-BIO-HBA1C", schema);

        assertEquals(1, filtered.path("fields").size());
        assertEquals("hba1c", filtered.path("fields").get(0).path("key").asText());
        assertFalse(filtered.has("rules"));
        assertEquals("SINGLE_ANALYTE", filtered.path("selectionMode").asText());
    }
}
