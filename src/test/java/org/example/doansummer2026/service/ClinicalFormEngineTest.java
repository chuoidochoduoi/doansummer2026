package org.example.doansummer2026.service;

import tools.jackson.databind.ObjectMapper;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.exception.BadRequestException;
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
}
