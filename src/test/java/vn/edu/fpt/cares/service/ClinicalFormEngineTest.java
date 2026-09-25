package vn.edu.fpt.cares.service;

import tools.jackson.databind.ObjectMapper;
import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalFormEngineTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ClinicalFormEngine engine = new ClinicalFormEngine();

    @Test
    void rejectsUnknownKey() throws Exception {
        var schema = mapper.readTree("{\"fields\":[{\"key\":\"glucose\",\"label\":\"Glucose\",\"type\":\"NUMBER\"}]}");
        var input = mapper.readTree("{\"unknown\":10}");
        assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(
                schema, input, LocalDate.of(1990, 1, 1), Gender.FEMALE, LocalDate.of(2026, 1, 1)));
    }

    @Test
    void calculatesAdultEgfrFromMicromolCreatinine() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"creatinine","label":"Creatinine","type":"NUMBER"},
          {"key":"egfr","label":"eGFR","type":"NUMBER","calculatorKey":"EGFR_CKD_EPI_2021_V1","precision":2}
        ]}
        """);
        var result = engine.validateAndEnrich(schema, mapper.readTree("{\"creatinine\":88.4}"),
                LocalDate.of(1990, 1, 1), Gender.FEMALE, LocalDate.of(2026, 1, 1));
        assertTrue(result.path("egfr").asDouble() > 70);
        assertTrue(result.path("egfr").asDouble() < 90);
        assertEquals("ESTIMATED", result.path("_meta").path("calculations").path("egfr").path("status").asText());
    }

    @Test
    void doesNotCalculateEgfrForChild() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"creatinine","label":"Creatinine","type":"NUMBER"},
          {"key":"egfr","label":"eGFR","type":"NUMBER","calculatorKey":"EGFR_CKD_EPI_2021_V1"}
        ]}
        """);
        var result = engine.validateAndEnrich(schema, mapper.readTree("{\"creatinine\":60}"),
                LocalDate.of(2012, 1, 1), Gender.MALE, LocalDate.of(2026, 1, 1));
        assertFalse(result.has("egfr"));
        assertEquals("NOT_CALCULATED", result.path("_meta").path("calculations").path("egfr").path("status").asText());
    }

    @Test
    void calculatesGestationalAgeAndEfwWithoutOverwritingClinicalAge() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"crl","label":"CRL","type":"NUMBER"},{"key":"bpd","label":"BPD","type":"NUMBER"},
          {"key":"hc","label":"HC","type":"NUMBER"},{"key":"ac","label":"AC","type":"NUMBER"},
          {"key":"fl","label":"FL","type":"NUMBER"},{"key":"clinicalGaWeeks","label":"Clinical GA","type":"NUMBER"},
          {"key":"gaCrl","label":"GA CRL","type":"NUMBER","calculatorKey":"GA_CRL_ROBINSON_FLEMING_V1","precision":0},
          {"key":"gaBpdFl","label":"GA BPD FL","type":"NUMBER","calculatorKey":"GA_HADLOCK_BPD_FL_V1","precision":0},
          {"key":"efw","label":"EFW","type":"NUMBER","calculatorKey":"EFW_HADLOCK_HC_AC_FL_V1","precision":0}
        ]}
        """);
        var input = mapper.readTree("{\"crl\":25,\"bpd\":45,\"hc\":170,\"ac\":150,\"fl\":30,\"clinicalGaWeeks\":19}");
        var result = engine.validateAndEnrich(schema, input, null, Gender.FEMALE, LocalDate.of(2026, 1, 1));
        assertEquals(64, result.path("gaCrl").asInt());
        assertFalse(result.has("gaBpdFl"));
        assertTrue(result.path("efw").asDouble() > 0);
        assertEquals(19, result.path("clinicalGaWeeks").asInt());
    }

    @Test
    void acceptsRequiredFieldMarkedAsNotPerformedAndMarksPartialCompletion() throws Exception {
        var schema = mapper.readTree("""
        {"layout":"LAB_TABLE","fields":[
          {"key":"glucose","label":"Đường huyết","type":"NUMBER","requiredOnSign":true},
          {"key":"creatinine","label":"Creatinine","type":"NUMBER","requiredOnSign":true}
        ]}
        """);
        var input = mapper.readTree("""
        {"creatinine":88.4,"_omissions":{"glucose":{"reasonCode":"INSUFFICIENT_SAMPLE"}}}
        """);

        var result = engine.validateAndEnrich(schema, input,
                LocalDate.of(1990, 1, 1), Gender.MALE, LocalDate.of(2026, 9, 2), true);

        assertFalse(result.has("glucose"));
        assertEquals("INSUFFICIENT_SAMPLE", result.path("_omissions").path("glucose").path("reasonCode").asText());
        assertEquals("PARTIAL", result.path("_meta").path("completionStatus").asText());
        assertEquals(1, result.path("_meta").path("omittedCount").asInt());
    }

    @Test
    void rejectsOmissionWithoutAValidReason() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[{"key":"glucose","label":"Đường huyết","type":"NUMBER","requiredOnSign":true}]}
        """);
        var input = mapper.readTree("""
        {"_omissions":{"glucose":{"reasonCode":""}}}
        """);

        assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(
                schema, input, null, Gender.FEMALE, LocalDate.of(2026, 9, 2), true));
    }

    @Test
    void schemaValidationRejectsInvalidShapesKeysLabelsTypesAndDuplicateKeys() throws Exception {
        for (String schema : new String[]{
                "null", "[]", "{}",
                "{\"fields\":[{\"key\":\"A\",\"label\":\"A\",\"type\":\"TEXT\"}]}",
                "{\"fields\":[{\"key\":\"validKey\",\"label\":\" \",\"type\":\"TEXT\"}]}",
                "{\"fields\":[{\"key\":\"validKey\",\"label\":\"X\",\"type\":\"FILE\"}]}",
                "{\"fields\":[{\"key\":\"validKey\",\"label\":\"X\",\"type\":\"TEXT\"},{\"key\":\"validKey\",\"label\":\"Y\",\"type\":\"TEXT\"}]}"
        }) {
            assertThrows(BadRequestException.class, () -> engine.validateSchema(mapper.readTree(schema)), schema);
        }
    }

    @Test
    void schemaValidationRejectsInvalidSelectCalculatorPatternAndConditions() throws Exception {
        for (String schema : new String[]{
                "{\"fields\":[{\"key\":\"choice\",\"label\":\"Chọn\",\"type\":\"SELECT\"}]}",
                "{\"fields\":[{\"key\":\"calc\",\"label\":\"Tính\",\"type\":\"NUMBER\",\"calculatorKey\":\"UNKNOWN\"}]}",
                "{\"fields\":[{\"key\":\"textValue\",\"label\":\"Text\",\"type\":\"TEXT\",\"pattern\":\"[\"}]}",
                "{\"fields\":[{\"key\":\"firstKey\",\"label\":\"A\",\"type\":\"TEXT\",\"visibleWhen\":[]}]}",
                "{\"fields\":[{\"key\":\"firstKey\",\"label\":\"A\",\"type\":\"TEXT\"},{\"key\":\"nextKey\",\"label\":\"B\",\"type\":\"TEXT\",\"requiredWhen\":{\"field\":\"missing\",\"equals\":true}}]}"
        }) {
            assertThrows(BadRequestException.class, () -> engine.validateSchema(mapper.readTree(schema)), schema);
        }
    }

    @Test
    void validatesAllPrimitiveTypesRangesSelectDateAndPattern() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"numberValue","label":"Số","type":"NUMBER","min":1,"max":10},
          {"key":"boolValue","label":"Cờ","type":"BOOLEAN"},
          {"key":"textValue","label":"Mã","type":"TEXT","pattern":"[A-Z]{2}","patternMessage":"Mã sai"},
          {"key":"dateValue","label":"Ngày","type":"DATE"},
          {"key":"choice","label":"Chọn","type":"SELECT","options":["A",{"value":"B","label":"Bờ"}]}
        ]}
        """);
        var valid = engine.validateAndEnrich(schema,
                mapper.readTree("{\"numberValue\":5,\"boolValue\":true,\"textValue\":\"AB\",\"dateValue\":\"2026-09-05\",\"choice\":\"B\"}"),
                null, null, LocalDate.of(2026, 9, 5));
        assertEquals("COMPLETE", valid.path("_meta").path("completionStatus").asText());
        for (String input : new String[]{
                "{\"numberValue\":\"5\"}", "{\"numberValue\":0}", "{\"numberValue\":11}",
                "{\"boolValue\":\"true\"}", "{\"textValue\":4}", "{\"textValue\":\"a1\"}",
                "{\"dateValue\":\"05/09/2026\"}", "{\"choice\":\"C\"}"
        }) {
            assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(schema,
                    mapper.readTree(input), null, null, LocalDate.of(2026, 9, 5), false), input);
        }
    }

    @Test
    void draftAllowsRequiredBlankWhileSignRequiresVisibleOrConditionalFields() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"hasSymptom","label":"Có triệu chứng","type":"BOOLEAN","required":true},
          {"key":"detail","label":"Chi tiết","type":"TEXTAREA","visibleWhen":{"field":"hasSymptom","equals":true},
           "requiredWhen":{"field":"hasSymptom","equals":true}},
          {"key":"hidden","label":"Ẩn","type":"TEXT","visibleWhen":{"field":"hasSymptom","equals":true},"required":true}
        ]}
        """);
        assertDoesNotThrow(() -> engine.validateAndEnrich(schema, mapper.readTree("{}"), null, null, null, false));
        assertThrows(BadRequestException.class,
                () -> engine.validateAndEnrich(schema, mapper.readTree("{}"), null, null, null, true));
        assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(schema,
                mapper.readTree("{\"hasSymptom\":true}"), null, null, null, true));
        var result = engine.validateAndEnrich(schema,
                mapper.readTree("{\"hasSymptom\":false,\"hidden\":\"remove me\"}"), null, null, null, true);
        assertFalse(result.has("detail"));
        assertFalse(result.has("hidden"));
    }

    @Test
    void inputMustBeObjectAndIncomingMetaIsRebuilt() throws Exception {
        var schema = mapper.readTree("{\"fields\":[{\"key\":\"textValue\",\"label\":\"Text\",\"type\":\"TEXT\"}]}");
        assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(schema,
                mapper.readTree("[]"), null, null, null));
        var result = engine.validateAndEnrich(schema,
                mapper.readTree("{\"textValue\":\"ok\",\"_meta\":{\"forged\":true}}"), null, null, null);
        assertFalse(result.path("_meta").has("forged"));
        assertDoesNotThrow(() -> engine.validateAndEnrich(schema, null, null, null, null, false));
    }

    @Test
    void omissionValidationCoversShapeUnknownCalculatedOtherAndDetail() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"glucose","label":"Đường huyết","type":"NUMBER","requiredOnSign":true},
          {"key":"bun","label":"BUN","type":"NUMBER","calculatorKey":"BUN_FROM_UREA_V1"},
          {"key":"urea","label":"Urea","type":"NUMBER"}
        ]}
        """);
        for (String input : new String[]{
                "{\"_omissions\":[]}",
                "{\"_omissions\":{\"unknown\":{\"reasonCode\":\"OTHER\",\"reasonDetail\":\"x\"}}}",
                "{\"_omissions\":{\"bun\":{\"reasonCode\":\"OTHER\",\"reasonDetail\":\"x\"}}}",
                "{\"_omissions\":{\"glucose\":\"OTHER\"}}",
                "{\"_omissions\":{\"glucose\":{\"reasonCode\":\"OTHER\"}}}"
        }) assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(schema,
                mapper.readTree(input), null, null, null, true), input);

        var result = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"_omissions\":{\"glucose\":{\"reasonCode\":\" other \",\"reasonDetail\":\" máy bảo trì \"}}}"),
                null, null, null, true);
        assertEquals("OTHER", result.path("_omissions").path("glucose").path("reasonCode").asText());
        assertEquals("máy bảo trì", result.path("_omissions").path("glucose").path("reasonDetail").asText());
    }

    @Test
    void evaluatesNumericTextualCriticalGenderAgeAndConditionalRanges() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"fasting","label":"Nhịn đói","type":"BOOLEAN"},
          {"key":"value","label":"Giá trị","type":"NUMBER","referenceRanges":[
            {"sex":"ANY","low":2,"high":8},
            {"sex":"FEMALE","minAge":18,"maxAge":60,"ageUnit":"YEARS","when":{"field":"fasting","equals":true},"low":3,"high":6}],
            "criticalRanges":[{"sex":"ANY","low":1,"high":10}]},
          {"key":"textResult","label":"Kết quả","type":"TEXT","referenceRanges":[{"normalValues":["NEGATIVE"]}]}
        ]}
        """);
        var high = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"fasting\":true,\"value\":11,\"textResult\":\"positive\"}"),
                LocalDate.of(1990, 1, 1), Gender.FEMALE, LocalDate.of(2026, 1, 1));
        assertEquals("CRITICAL_HIGH", high.path("_meta").path("flags").path("value").path("status").asText());
        assertEquals("ABNORMAL", high.path("_meta").path("flags").path("textResult").path("status").asText());
        var low = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"fasting\":false,\"value\":0.5,\"textResult\":\"negative\"}"),
                LocalDate.of(2025, 1, 1), Gender.MALE, LocalDate.of(2026, 1, 1));
        assertEquals("CRITICAL_LOW", low.path("_meta").path("flags").path("value").path("status").asText());
        assertEquals("NORMAL", low.path("_meta").path("flags").path("textResult").path("status").asText());
    }

    @Test
    void evaluatesQualifierRangesAndNotEvaluatedRange() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"qualifier","label":"Dấu","type":"SELECT","options":["LESS_THAN","GREATER_THAN","EQUAL"]},
          {"key":"value","label":"Giá trị","type":"NUMBER","qualifierKey":"qualifier","referenceRanges":[{"high":5}]},
          {"key":"noRange","label":"Không range","type":"NUMBER","referenceRanges":[]}
        ]}
        """);
        var less = engine.validateAndEnrich(schema,
                mapper.readTree("{\"qualifier\":\"LESS_THAN\",\"value\":5,\"noRange\":1}"), null, null, null);
        assertEquals("NORMAL", less.path("_meta").path("flags").path("value").path("status").asText());
        assertEquals("NOT_EVALUATED", less.path("_meta").path("flags").path("noRange").path("status").asText());
        var greater = engine.validateAndEnrich(schema,
                mapper.readTree("{\"qualifier\":\"GREATER_THAN\",\"value\":5}"), null, null, null);
        assertEquals("HIGH", greater.path("_meta").path("flags").path("value").path("status").asText());
    }

    @Test
    void calculatesAllDerivedChemistryValuesAndRejectsImpossibleIndirectValue() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"totalCholesterol","label":"TC","type":"NUMBER"},{"key":"hdlC","label":"HDL","type":"NUMBER"},
          {"key":"bilirubinTotal","label":"BT","type":"NUMBER"},{"key":"bilirubinDirect","label":"BD","type":"NUMBER"},
          {"key":"urea","label":"Urea","type":"NUMBER"},
          {"key":"nonHdl","label":"non HDL","type":"NUMBER","calculatorKey":"NON_HDL_C_V1"},
          {"key":"indirect","label":"BI","type":"NUMBER","calculatorKey":"INDIRECT_BILIRUBIN_V1"},
          {"key":"bun","label":"BUN","type":"NUMBER","calculatorKey":"BUN_FROM_UREA_V1"}
        ]}
        """);
        var result = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"totalCholesterol\":5.2,\"hdlC\":1.2,\"bilirubinTotal\":20,\"bilirubinDirect\":5,\"urea\":4}"),
                null, null, null);
        assertEquals(4.0, result.path("nonHdl").asDouble(), 0.001);
        assertEquals(15.0, result.path("indirect").asDouble(), 0.001);
        assertEquals(11.2, result.path("bun").asDouble(), 0.01);
        var impossible = engine.validateAndEnrich(schema,
                mapper.readTree("{\"bilirubinTotal\":2,\"bilirubinDirect\":5}"), null, null, null);
        assertFalse(impossible.has("indirect"));
    }

    @Test
    void calculatesBpdFlWhenCrlUnavailableAndHandlesInvalidInputs() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"crl","label":"CRL","type":"NUMBER"},{"key":"bpd","label":"BPD","type":"NUMBER"},
          {"key":"fl","label":"FL","type":"NUMBER"},{"key":"hc","label":"HC","type":"NUMBER"},
          {"key":"ac","label":"AC","type":"NUMBER"},
          {"key":"ga","label":"GA","type":"NUMBER","calculatorKey":"GA_HADLOCK_BPD_FL_V1"},
          {"key":"efw","label":"EFW","type":"NUMBER","calculatorKey":"EFW_HADLOCK_HC_AC_FL_V1"}
        ]}
        """);
        var result = engine.validateAndEnrich(schema,
                mapper.readTree("{\"crl\":100,\"bpd\":45,\"fl\":30,\"hc\":170,\"ac\":150}"), null, null, null);
        assertTrue(result.has("ga"));
        assertTrue(result.has("efw"));
        var missing = engine.validateAndEnrich(schema,
                mapper.readTree("{\"bpd\":0,\"fl\":0,\"hc\":0,\"ac\":0}"), null, null, null);
        assertFalse(missing.has("ga"));
        assertFalse(missing.has("efw"));
    }

    @Test
    void validatesConsistencyRulesAsWarningsErrorsAndDraftOnlyBehavior() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"aa","label":"A","type":"NUMBER"},{"key":"bb","label":"B","type":"NUMBER"},
          {"key":"total","label":"Tổng","type":"NUMBER"},{"key":"percent","label":"%","type":"NUMBER"},
          {"key":"absolute","label":"SL","type":"NUMBER"},{"key":"flagA","label":"A?","type":"BOOLEAN"},
          {"key":"flagB","label":"B?","type":"BOOLEAN"},{"key":"confirm","label":"Xác nhận","type":"BOOLEAN"},
          {"key":"choice","label":"Chọn","type":"TEXT"}
        ],"rules":[
          {"type":"SUM_BETWEEN","keys":["aa","bb"],"min":99,"max":101,"message":"Tổng sai"},
          {"type":"LESS_THAN_OR_EQUAL","left":"aa","right":"bb","message":"A lớn hơn B"},
          {"type":"ABSOLUTE_FROM_PERCENT","total":"total","pairs":[{"percent":"percent","absolute":"absolute"}],"message":"Tuyệt đối sai"},
          {"type":"AT_LEAST_ONE_TRUE","keys":["flagA","flagB"],"message":"Cần chọn"},
          {"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"confirm","when":{"field":"flagA","equals":true},"message":"Cần xác nhận"},
          {"type":"VALUE_NOT_ALLOWED_WHEN","field":"choice","value":"NO","when":{"field":"flagB","equals":true},"message":"Không được NO"},
          {"type":"AT_LEAST_ONE_TRUE","keys":["flagA"],"severity":"ERROR","onSignOnly":true,"message":"Lỗi khi ký"}
        ]}
        """);
        var draft = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"aa\":70,\"bb\":20,\"total\":100,\"percent\":50,\"absolute\":1,\"flagA\":false,\"flagB\":true,\"confirm\":false,\"choice\":\"NO\"}"),
                null, null, null, false);
        assertTrue(draft.path("_meta").path("warnings").size() >= 3);
        assertThrows(BadRequestException.class, () -> engine.validateAndEnrich(schema, mapper.readTree(
                "{\"aa\":50,\"bb\":50,\"flagA\":false}"), null, null, null, true));
    }

    @Test
    void sectionsAndInConditionAreSupported() throws Exception {
        var schema = mapper.readTree("""
        {"sections":[{"fields":[
          {"key":"kind","label":"Loại","type":"SELECT","options":["A","B","C"]},
          {"key":"detail","label":"Chi tiết","type":"TEXT","visibleWhen":{"field":"kind","in":["A","B"]}}
        ]}]}
        """);
        var shown = engine.validateAndEnrich(schema, mapper.readTree("{\"kind\":\"A\",\"detail\":\"ok\"}"), null, null, null);
        assertTrue(shown.has("detail"));
        var hidden = engine.validateAndEnrich(schema, mapper.readTree("{\"kind\":\"C\",\"detail\":\"remove\"}"), null, null, null);
        assertFalse(hidden.has("detail"));
    }

    @Test
    void coversRangeSelectionQualifiersAgeUnitsAndConditionVariants() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"mode","label":"Chế độ","type":"TEXT"},
          {"key":"qualifier","label":"Dấu","type":"SELECT","options":["LESS_THAN","GREATER_THAN","EQUAL"]},
          {"key":"value","label":"Giá trị","type":"NUMBER","qualifierKey":"qualifier","referenceRanges":[
            {"sex":"MALE","minAge":1,"maxAge":24,"ageUnit":"MONTHS","when":{"field":"mode","notEquals":"OFF"},"low":2,"high":8},
            {"sex":"MALE","minAge":300,"maxAge":900,"ageUnit":"DAYS","low":1,"high":9},
            {"sex":"FEMALE","low":3,"high":7}]},
          {"key":"visible","label":"Hiện","type":"TEXT","visibleWhen":{"field":"mode"}},
          {"key":"empty","label":"Rỗng","type":"TEXT"}
        ]}
        """);
        var male = engine.validateAndEnrich(schema,
                mapper.readTree("{\"mode\":\"ON\",\"qualifier\":\"EQUAL\",\"value\":1,\"visible\":\"x\",\"empty\":\" \"}"),
                LocalDate.of(2025, 1, 1), Gender.MALE, LocalDate.of(2026, 1, 1));
        assertEquals("LOW", male.path("_meta").path("flags").path("value").path("status").asText());
        assertTrue(male.has("visible"));

        var lessNotNormal = engine.validateAndEnrich(schema,
                mapper.readTree("{\"mode\":\"OFF\",\"qualifier\":\"LESS_THAN\",\"value\":10}"),
                null, Gender.MALE, null);
        assertEquals("NOT_EVALUATED", lessNotNormal.path("_meta").path("flags").path("value").path("status").asText());
        assertFalse(lessNotNormal.has("visible"));

        var greaterWithoutHigh = mapper.readTree("""
          {"fields":[{"key":"qq","label":"Q","type":"TEXT"},
          {"key":"vv","label":"V","type":"NUMBER","qualifierKey":"qq","referenceRanges":[{"low":2}]}]}
        """);
        var unresolved = engine.validateAndEnrich(greaterWithoutHigh,
                mapper.readTree("{\"qq\":\"GREATER_THAN\",\"vv\":3}"), null, null, null);
        assertEquals("NOT_EVALUATED", unresolved.path("_meta").path("flags").path("vv").path("status").asText());
    }

    @Test
    void coversCalculationBoundaryAndMissingInputBranches() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"creatinine","label":"Creatinine","type":"NUMBER"},
          {"key":"crl","label":"CRL","type":"NUMBER"},{"key":"bpd","label":"BPD","type":"NUMBER"},
          {"key":"fl","label":"FL","type":"NUMBER"},{"key":"hc","label":"HC","type":"NUMBER"},
          {"key":"ac","label":"AC","type":"NUMBER"},{"key":"totalCholesterol","label":"TC","type":"NUMBER"},
          {"key":"hdlC","label":"HDL","type":"NUMBER"},{"key":"urea","label":"Urea","type":"NUMBER"},
          {"key":"egfr","label":"eGFR","type":"NUMBER","calculatorKey":"EGFR_CKD_EPI_2021_V1"},
          {"key":"gaCrl","label":"GA1","type":"NUMBER","calculatorKey":"GA_CRL_ROBINSON_FLEMING_V1"},
          {"key":"ga","label":"GA2","type":"NUMBER","calculatorKey":"GA_HADLOCK_BPD_FL_V1"},
          {"key":"efw","label":"EFW","type":"NUMBER","calculatorKey":"EFW_HADLOCK_HC_AC_FL_V1"},
          {"key":"nonHdl","label":"NHDL","type":"NUMBER","calculatorKey":"NON_HDL_C_V1"},
          {"key":"bun","label":"BUN","type":"NUMBER","calculatorKey":"BUN_FROM_UREA_V1"}
        ]}
        """);
        var male = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"creatinine\":110,\"crl\":5,\"bpd\":45,\"fl\":30,\"hc\":170,\"ac\":150,\"totalCholesterol\":5,\"hdlC\":1,\"urea\":4}"),
                LocalDate.of(1980, 1, 1), Gender.MALE, LocalDate.of(2026, 1, 1));
        assertTrue(male.has("egfr"));
        assertTrue(male.has("gaCrl"));
        assertFalse(male.has("ga"));

        var missing = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"creatinine\":0,\"crl\":4,\"bpd\":200,\"fl\":200,\"hc\":0,\"ac\":1}"),
                LocalDate.of(1980, 1, 1), Gender.MALE, LocalDate.of(2026, 1, 1));
        assertFalse(missing.has("egfr"));
        assertFalse(missing.has("gaCrl"));
        assertFalse(missing.has("ga"));
        assertFalse(missing.has("efw"));
        assertFalse(missing.has("nonHdl"));
        assertFalse(missing.has("bun"));
    }

    @Test
    void coversNonViolatingAndIncompleteConsistencyRules() throws Exception {
        var schema = mapper.readTree("""
        {"fields":[
          {"key":"aa","label":"A","type":"NUMBER"},{"key":"bb","label":"B","type":"NUMBER"},
          {"key":"total","label":"T","type":"NUMBER"},{"key":"percent","label":"P","type":"NUMBER"},
          {"key":"absolute","label":"X","type":"NUMBER"},{"key":"flag","label":"F","type":"BOOLEAN"},
          {"key":"confirm","label":"C","type":"BOOLEAN"},{"key":"choice","label":"V","type":"TEXT"}
        ],"rules":[
          {"type":"SUM_BETWEEN","keys":["aa","bb"],"min":9,"max":11},
          {"type":"LESS_THAN_OR_EQUAL","left":"aa","right":"bb"},
          {"type":"ABSOLUTE_FROM_PERCENT","total":"total","pairs":[{"percent":"percent","absolute":"absolute"}]},
          {"type":"AT_LEAST_ONE_TRUE","keys":["flag"]},
          {"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"confirm","when":{"field":"flag","equals":true}},
          {"type":"VALUE_NOT_ALLOWED_WHEN","field":"choice","value":"NO","when":{"field":"flag","equals":true}},
          {"type":"UNKNOWN"}
        ]}
        """);
        var complete = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"aa\":5,\"bb\":5,\"total\":100,\"percent\":50,\"absolute\":50,\"flag\":true,\"confirm\":true,\"choice\":\"YES\"}"),
                null, null, null);
        assertEquals(0, complete.path("_meta").path("warnings").size());
        var incomplete = engine.validateAndEnrich(schema, mapper.readTree("{\"aa\":5,\"flag\":false}"),
                null, null, null, false);
        assertTrue(incomplete.path("_meta").path("warnings").size() >= 1);
    }

    @Test
    void coversSchemaAndValueBoundaryBranches() throws Exception {
        assertThrows(BadRequestException.class, () -> engine.validateSchema(mapper.readTree("null")));
        assertThrows(BadRequestException.class, () -> engine.validateSchema(mapper.readTree("{}")));
        for (String schema : new String[]{
                "{\"fields\":[{\"key\":\"a\",\"label\":\"A\",\"type\":\"TEXT\"},{\"key\":\"a\",\"label\":\"B\",\"type\":\"TEXT\"}]}",
                "{\"fields\":[{\"key\":\"Bad\",\"label\":\"A\",\"type\":\"TEXT\"}]}",
                "{\"fields\":[{\"key\":\"aa\",\"type\":\"TEXT\"}]}",
                "{\"fields\":[{\"key\":\"aa\",\"label\":\"A\",\"type\":\"UNKNOWN\"}]}",
                "{\"fields\":[{\"key\":\"aa\",\"label\":\"A\",\"type\":\"TEXT\",\"visibleWhen\":{}}]}"
        }) assertThrows(BadRequestException.class, () -> engine.validateSchema(mapper.readTree(schema)));

        var schema = mapper.readTree("""
          {"fields":[
            {"key":"aa","label":"A","type":"TEXT"},
            {"key":"bb","label":"B","type":"NUMBER","referenceRanges":[{"low":2}]},
            {"key":"cc","label":"C","type":"NUMBER","referenceRanges":[{"high":8}]},
            {"key":"dd","label":"D","type":"TEXT","referenceRanges":[{"normalValues":"NEGATIVE"}]}
          ]}
        """);
        var result = engine.validateAndEnrich(schema,
                mapper.readTree("{\"aa\":null,\"bb\":3,\"cc\":7,\"dd\":\"NEGATIVE\"}"), null, null, null);
        assertEquals("NORMAL", result.path("_meta").path("flags").path("bb").path("status").asText());
        assertEquals("NORMAL", result.path("_meta").path("flags").path("cc").path("status").asText());
        assertEquals("NOT_EVALUATED", result.path("_meta").path("flags").path("dd").path("status").asText());
    }

    @Test
    void coversCalculatorSexAgeAndMissingOperandBranches() throws Exception {
        var schema = mapper.readTree("""
          {"fields":[
            {"key":"creatinine","label":"Creatinine","type":"NUMBER"},
            {"key":"crl","label":"CRL","type":"NUMBER"},
            {"key":"bpd","label":"BPD","type":"NUMBER"},{"key":"fl","label":"FL","type":"NUMBER"},
            {"key":"hc","label":"HC","type":"NUMBER"},{"key":"ac","label":"AC","type":"NUMBER"},
            {"key":"totalCholesterol","label":"TC","type":"NUMBER"},{"key":"hdlC","label":"HDL","type":"NUMBER"},
            {"key":"egfr","label":"eGFR","type":"NUMBER","calculatorKey":"EGFR_CKD_EPI_2021_V1"},
            {"key":"ga1","label":"GA1","type":"NUMBER","calculatorKey":"GA_CRL_ROBINSON_FLEMING_V1"},
            {"key":"ga2","label":"GA2","type":"NUMBER","calculatorKey":"GA_HADLOCK_BPD_FL_V1"},
            {"key":"efw","label":"EFW","type":"NUMBER","calculatorKey":"EFW_HADLOCK_HC_AC_FL_V1"},
            {"key":"nonHdl","label":"NHDL","type":"NUMBER","calculatorKey":"NON_HDL_C_V1"}
          ]}
        """);
        var female = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"creatinine\":70,\"crl\":10,\"bpd\":45,\"fl\":30,\"hc\":170,\"ac\":150,\"totalCholesterol\":5,\"hdlC\":1}"),
                LocalDate.of(1990, 1, 1), Gender.FEMALE, LocalDate.of(2026, 1, 1));
        assertTrue(female.has("egfr"));
        assertTrue(female.has("ga1"));
        assertFalse(female.has("ga2"));

        var adolescent = engine.validateAndEnrich(schema, mapper.readTree(
                "{\"creatinine\":70,\"bpd\":1,\"fl\":1,\"hc\":170,\"ac\":150,\"totalCholesterol\":5}"),
                LocalDate.of(2012, 1, 1), Gender.FEMALE, LocalDate.of(2026, 1, 1));
        assertFalse(adolescent.has("egfr"));
        assertFalse(adolescent.has("ga2"));
        assertFalse(adolescent.has("nonHdl"));

        for (String data : new String[]{
                "{\"bpd\":45,\"fl\":30,\"hc\":170,\"ac\":0}",
                "{\"bpd\":45,\"fl\":30,\"hc\":0,\"ac\":150}",
                "{\"bpd\":45,\"fl\":30,\"ac\":150}"
        }) assertFalse(engine.validateAndEnrich(schema, mapper.readTree(data), null, null, null).has("efw"));
    }

    @Test
    void coversRuleConditionsWithMissingValuesAndShortCircuiting() throws Exception {
        var schema = mapper.readTree("""
          {"fields":[
            {"key":"total","label":"T","type":"NUMBER"},{"key":"pct","label":"P","type":"NUMBER"},
            {"key":"absolute","label":"A","type":"NUMBER"},{"key":"flag","label":"F","type":"BOOLEAN"},
            {"key":"confirm","label":"C","type":"BOOLEAN"},{"key":"choice","label":"V","type":"TEXT"}
          ],"rules":[
            {"type":"SUM_BETWEEN","keys":"invalid"},
            {"type":"ABSOLUTE_FROM_PERCENT","total":"total","pairs":[{"percent":"pct","absolute":"absolute"}]},
            {"type":"AT_LEAST_ONE_TRUE","keys":"invalid"},
            {"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"confirm","when":{"field":"flag","equals":true}},
            {"type":"VALUE_NOT_ALLOWED_WHEN","field":"choice","value":"NO","when":{"field":"flag","equals":true}}
          ]}
        """);
        var missing = engine.validateAndEnrich(schema, mapper.readTree("{}"), null, null, null, false);
        assertEquals(0, missing.path("_meta").path("warnings").size());
        var partial = engine.validateAndEnrich(schema,
                mapper.readTree("{\"total\":100,\"pct\":50,\"flag\":true}"), null, null, null, false);
        assertEquals(1, partial.path("_meta").path("warnings").size());
    }
}
