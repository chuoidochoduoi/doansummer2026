package org.example.doansummer2026.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Danh mục chỉ số thu tiền dùng serviceCode để không phụ thuộc UUID/database.
 * Các trường tính toán và trường bối cảnh không nằm trong danh mục này.
 */
public final class LaboratoryAnalyteCatalog {
    private LaboratoryAnalyteCatalog() {}

    public record Analyte(String serviceCode, String fieldKey, String name, BigDecimal demoPrice) {}
    public record Panel(String serviceCode, String name, List<Analyte> analytes) {}

    private static Analyte a(String code, String key, String name, int price) {
        return new Analyte(code, key, name, BigDecimal.valueOf(price));
    }

    public static final List<Panel> PANELS = List.of(
            new Panel("LAB-001", "Công thức máu", List.of(
                    a("AN-CBC-RBC", "rbc", "Số lượng hồng cầu (RBC)", 5000),
                    a("AN-CBC-HGB", "hgb", "Huyết sắc tố (HGB)", 5000),
                    a("AN-CBC-HCT", "hct", "Hematocrit (HCT)", 5000),
                    a("AN-CBC-MCV", "mcv", "MCV", 5000),
                    a("AN-CBC-WBC", "wbc", "Số lượng bạch cầu (WBC)", 10000),
                    a("AN-CBC-NEUTP", "neutPercent", "Bạch cầu trung tính (%)", 5000),
                    a("AN-CBC-LYMP", "lymphPercent", "Bạch cầu lympho (%)", 5000),
                    a("AN-CBC-PLT", "plt", "Số lượng tiểu cầu (PLT)", 10000)
            )),
            new Panel("LAB-003", "Sinh hóa máu cơ bản", List.of(
                    a("AN-BIO-HBA1C", "hba1c", "Hemoglobin A1c", 30000),
                    a("AN-BIO-TC", "totalCholesterol", "Cholesterol toàn phần", 30000),
                    a("AN-BIO-TG", "triglyceride", "Triglyceride", 30000),
                    a("AN-BIO-HDL", "hdlC", "HDL Cholesterol", 30000),
                    a("AN-BIO-LDL", "ldlC", "LDL Cholesterol", 30000),
                    a("AN-BIO-UA", "acidUric", "Acid uric", 30000),
                    a("AN-BIO-TP", "totalProtein", "Protein toàn phần", 30000)
            )),
            new Panel("LAB-004", "Chức năng gan", List.of(
                    a("AN-LIV-AST", "ast", "AST", 25000),
                    a("AN-LIV-ALT", "alt", "ALT", 25000),
                    a("AN-LIV-ALP", "alp", "Alkaline phosphatase", 25000),
                    a("AN-LIV-GGT", "ggt", "GGT", 25000),
                    a("AN-LIV-TBIL", "bilirubinTotal", "Bilirubin toàn phần", 25000),
                    a("AN-LIV-DBIL", "bilirubinDirect", "Bilirubin trực tiếp", 25000),
                    a("AN-LIV-ALB", "albumin", "Albumin", 25000)
            )),
            new Panel("LAB-005", "Chức năng thận", List.of(
                    a("AN-REN-UREA", "urea", "Urea", 20000),
                    a("AN-REN-CREA", "creatinine", "Creatinine", 20000),
                    a("AN-REN-NA", "sodium", "Sodium", 20000),
                    a("AN-REN-K", "potassium", "Potassium", 20000),
                    a("AN-REN-CL", "chloride", "Chloride", 20000),
                    a("AN-REN-CA", "calcium", "Calcium toàn phần", 20000)
            )),
            new Panel("LAB-006", "Tổng phân tích nước tiểu", List.of(
                    a("AN-URI-SG", "specificGravity", "Tỷ trọng nước tiểu", 10000),
                    a("AN-URI-PH", "ph", "pH nước tiểu", 10000),
                    a("AN-URI-LEU", "leukocyteEsterase", "Leukocyte Esterase", 10000),
                    a("AN-URI-NIT", "nitrite", "Nitrite", 10000),
                    a("AN-URI-PRO", "protein", "Protein nước tiểu", 10000),
                    a("AN-URI-GLU", "urineGlucose", "Glucose nước tiểu", 10000),
                    a("AN-URI-KET", "ketone", "Ketone", 10000),
                    a("AN-URI-BLD", "blood", "Máu/Hemoglobin nước tiểu", 10000)
            )),
            new Panel("LAB-007", "Xét nghiệm CRP", List.of(
                    a("AN-CRP", "crp", "CRP định lượng", 100000)
            ))
    );

    private static final Map<String, Panel> PANEL_BY_CODE = new LinkedHashMap<>();
    private static final Map<String, Panel> PANEL_BY_ANALYTE_CODE = new LinkedHashMap<>();
    private static final Map<String, Analyte> ANALYTE_BY_CODE = new LinkedHashMap<>();
    static {
        PANELS.forEach(panel -> {
            PANEL_BY_CODE.put(panel.serviceCode(), panel);
            panel.analytes().forEach(analyte -> {
                ANALYTE_BY_CODE.put(analyte.serviceCode(), analyte);
                PANEL_BY_ANALYTE_CODE.put(analyte.serviceCode(), panel);
            });
        });
    }

    public static Optional<Panel> panel(String serviceCode) {
        return Optional.ofNullable(PANEL_BY_CODE.get(normalize(serviceCode)));
    }

    public static Optional<Panel> parentPanel(String analyteServiceCode) {
        return Optional.ofNullable(PANEL_BY_ANALYTE_CODE.get(normalize(analyteServiceCode)));
    }

    public static Optional<Analyte> analyte(String serviceCode) {
        return Optional.ofNullable(ANALYTE_BY_CODE.get(normalize(serviceCode)));
    }

    public static JsonNode schemaForService(String serviceCode, JsonNode schema) {
        Optional<Panel> panel = panel(serviceCode);
        Optional<Analyte> analyte = analyte(serviceCode);
        if ((panel.isEmpty() && analyte.isEmpty()) || schema == null || !schema.isObject()) return schema;
        ObjectNode normalized = ensureCatalogFields(((ObjectNode) schema).deepCopy(),
                panel.or(() -> parentPanel(serviceCode)).orElseThrow());
        if (analyte.isEmpty()) return normalized;

        ObjectNode filtered = normalized.deepCopy();
        ArrayNode fields = filtered.arrayNode();
        JsonNode sourceFields = normalized.path("fields");
        if (sourceFields.isArray()) {
            sourceFields.forEach(field -> {
                if (analyte.get().fieldKey().equals(field.path("key").asText())) fields.add(field.deepCopy());
            });
        }
        filtered.set("fields", fields);
        filtered.remove("rules");
        filtered.put("selectionMode", "SINGLE_ANALYTE");
        filtered.put("sourcePanelCode", parentPanel(serviceCode).map(Panel::serviceCode).orElse(""));
        return filtered;
    }

    /**
     * Older template versions contain only a subset of a panel's analytes.
     * The billable catalogue is the source of truth for a panel workbench, so
     * append a safe numeric field for any missing analyte without changing the
     * stored template or database data.
     */
    private static ObjectNode ensureCatalogFields(ObjectNode schema, Panel panel) {
        ArrayNode fields = schema.withArray("fields");
        java.util.Set<String> keys = new java.util.HashSet<>();
        fields.forEach(field -> {
            String key = field.path("key").asText();
            keys.add(key);
            if (field instanceof ObjectNode object && panel.analytes().stream()
                    .anyMatch(analyte -> analyte.fieldKey().equals(key))) {
                // A purchased analyte must have either a value or an explicit
                // omission reason when the group is signed.
                object.put("required", true);
            }
        });
        int order = fields.size() + 1;
        for (Analyte analyte : panel.analytes()) {
            if (!keys.contains(analyte.fieldKey())) {
                ObjectNode field = fields.addObject();
                field.put("key", analyte.fieldKey());
                field.put("label", analyte.name());
                field.put("type", "NUMBER");
                field.put("required", true);
                field.put("displayOrder", order++);
            }
        }
        return schema;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
