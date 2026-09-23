package org.example.doansummer2026.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void schemaSelectionCoversUnknownNullNonObjectAndWholePanel() throws Exception {
        var object = mapper.readTree("{\"fields\":[]}");
        assertSame(object, LaboratoryAnalyteCatalog.schemaForService("UNKNOWN", object));
        assertSame(object, LaboratoryAnalyteCatalog.schemaForService(null, object));
        assertEquals(null, LaboratoryAnalyteCatalog.schemaForService("LAB-001", null));
        var scalar = mapper.readTree("\"value\"");
        assertSame(scalar, LaboratoryAnalyteCatalog.schemaForService("LAB-001", scalar));

        var panel = LaboratoryAnalyteCatalog.schemaForService("LAB-001", object);
        assertTrue(panel.path("fields").isArray());
        assertEquals(8, panel.path("fields").size());
        assertFalse(panel.has("selectionMode"));
        assertTrue(panel.path("fields").get(0).path("required").asBoolean());
    }

    @Test
    void crpPanelAndAnalyteUseTheSameConfiguredField() throws Exception {
        var schema = mapper.readTree("{\"fields\":[]}");

        var panel = LaboratoryAnalyteCatalog.schemaForService("LAB-007", schema);
        var analyte = LaboratoryAnalyteCatalog.schemaForService("AN-CRP", schema);

        assertEquals(1, panel.path("fields").size());
        assertEquals("crp", panel.path("fields").get(0).path("key").asText());
        assertEquals(1, analyte.path("fields").size());
        assertEquals("crp", analyte.path("fields").get(0).path("key").asText());
        assertEquals("SINGLE_ANALYTE", analyte.path("selectionMode").asText());
    }

    @Test
    void analyteSelectionHandlesMissingAndMixedSourceFields() throws Exception {
        var withoutFields = mapper.readTree("{}");
        var missing = LaboratoryAnalyteCatalog.schemaForService("AN-CBC-RBC", withoutFields);
        assertTrue(missing.path("fields").isArray());
        // The catalogue supplies a safe field when an older template omitted
        // the purchased analyte, then the single-analyte view keeps that field.
        assertEquals(1, missing.path("fields").size());
        assertEquals("rbc", missing.path("fields").get(0).path("key").asText());

        var mixed = mapper.readTree("""
                {"fields":[12,{"key":"unknown","label":"Không thuộc gói"},{"key":"rbc","label":"RBC"}]}
                """);
        var selected = LaboratoryAnalyteCatalog.schemaForService("AN-CBC-RBC", mixed);
        assertEquals(1, selected.path("fields").size());
        assertEquals("rbc", selected.path("fields").get(0).path("key").asText());
    }
}
