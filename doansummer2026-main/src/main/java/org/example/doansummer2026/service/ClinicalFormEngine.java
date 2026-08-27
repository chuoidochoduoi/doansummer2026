package org.example.doansummer2026.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class ClinicalFormEngine {
    private static final Set<String> TYPES = Set.of("TEXT", "TEXTAREA", "NUMBER", "DATE", "SELECT", "BOOLEAN");
    private static final Set<String> CALCULATORS = Set.of(
            "EGFR_CKD_EPI_2021_V1", "GA_CRL_ROBINSON_FLEMING_V1",
            "GA_HADLOCK_BPD_FL_V1", "EFW_HADLOCK_HC_AC_FL_V1");

    private ObjectNode objectNode() { return JsonNodeFactory.instance.objectNode(); }

    public void validateSchema(JsonNode schema) {
        if (schema == null || !schema.isObject()) throw new BadRequestException("Cấu trúc form phải là JSON object");
        List<JsonNode> fields = fields(schema);
        if (fields.isEmpty()) throw new BadRequestException("Form phải có ít nhất một trường");
        Set<String> keys = new HashSet<>();
        for (JsonNode field : fields) {
            String key = text(field, "key");
            String label = text(field, "label");
            String type = text(field, "type").toUpperCase(Locale.ROOT);
            if (!key.matches("[a-z][a-zA-Z0-9_]{1,63}")) throw new BadRequestException("Key không hợp lệ: " + key);
            if (!keys.add(key)) throw new BadRequestException("Key bị trùng: " + key);
            if (label.isBlank()) throw new BadRequestException("Trường " + key + " thiếu label");
            if (!TYPES.contains(type)) throw new BadRequestException("Kiểu trường không được hỗ trợ: " + type);
            if ("SELECT".equals(type) && (!field.has("options") || !field.get("options").isArray()))
                throw new BadRequestException("Trường select phải có options: " + key);
            if (field.hasNonNull("calculatorKey") && !CALCULATORS.contains(field.get("calculatorKey").asText()))
                throw new BadRequestException("Calculator không được hỗ trợ: " + field.get("calculatorKey").asText());
        }
    }

    public JsonNode validateAndEnrich(JsonNode schema, JsonNode input, LocalDate birthDate, Gender gender, LocalDate performedDate) {
        validateSchema(schema);
        if (input == null || input.isNull()) input = objectNode();
        if (!input.isObject()) throw new BadRequestException("Dữ liệu form phải là JSON object");
        ObjectNode output = ((ObjectNode) input).deepCopy();
        output.remove("_meta");
        Map<String, JsonNode> definitions = new LinkedHashMap<>();
        for (JsonNode f : fields(schema)) definitions.put(f.get("key").asText(), f);
        output.propertyNames().forEach(key -> {
            if (!definitions.containsKey(key)) throw new BadRequestException("Trường không thuộc template: " + key);
        });

        ObjectNode flags = objectNode();
        int age = birthDate == null ? -1 : Period.between(birthDate, performedDate == null ? LocalDate.now() : performedDate).getYears();
        for (Map.Entry<String, JsonNode> entry : definitions.entrySet()) {
            String key = entry.getKey();
            JsonNode f = entry.getValue();
            JsonNode value = output.get(key);
            boolean empty = value == null || value.isNull() || (value.isTextual() && value.asText().isBlank());
            if (f.path("required").asBoolean(false) && empty && !f.hasNonNull("calculatorKey"))
                throw new BadRequestException("Vui lòng nhập " + f.path("label").asText(key));
            if (!empty) validateValue(f, value);
            if (!empty && f.has("referenceRanges")) flags.set(key, evaluateRange(f.get("referenceRanges"), value, age, gender));
        }
        ObjectNode calculations = applyCalculations(definitions, output, age, gender);
        ObjectNode meta = objectNode();
        meta.set("flags", flags);
        meta.set("calculations", calculations);
        output.set("_meta", meta);
        return output;
    }

    private void validateValue(JsonNode field, JsonNode value) {
        String key = field.get("key").asText();
        String type = field.get("type").asText().toUpperCase(Locale.ROOT);
        if ("NUMBER".equals(type)) {
            if (!value.isNumber()) throw new BadRequestException(key + " phải là số");
            double v = value.asDouble();
            if (field.has("min") && v < field.get("min").asDouble()) throw new BadRequestException(key + " nhỏ hơn giới hạn");
            if (field.has("max") && v > field.get("max").asDouble()) throw new BadRequestException(key + " vượt giới hạn");
        } else if ("BOOLEAN".equals(type) && !value.isBoolean()) {
            throw new BadRequestException(key + " phải là true/false");
        } else if (("TEXT".equals(type) || "TEXTAREA".equals(type) || "DATE".equals(type) || "SELECT".equals(type)) && !value.isTextual()) {
            throw new BadRequestException(key + " phải là chuỗi");
        }
        if ("SELECT".equals(type)) {
            boolean allowed = false;
            for (JsonNode option : field.get("options")) {
                String candidate = option.isTextual() ? option.asText() : option.path("value").asText();
                if (candidate.equals(value.asText())) { allowed = true; break; }
            }
            if (!allowed) throw new BadRequestException("Giá trị không hợp lệ cho " + key);
        }
        if ("DATE".equals(type)) try { LocalDate.parse(value.asText()); }
        catch (Exception ex) { throw new BadRequestException(key + " phải theo định dạng YYYY-MM-DD"); }
    }

    private ObjectNode evaluateRange(JsonNode ranges, JsonNode value, int age, Gender gender) {
        ObjectNode flag = objectNode();
        JsonNode selected = null;
        int best = -1;
        for (JsonNode r : ranges) {
            String sex = r.path("sex").asText("ANY");
            boolean sexMatch = "ANY".equalsIgnoreCase(sex) || (gender != null && sex.equalsIgnoreCase(gender.name()));
            boolean ageMatch = age < 0 || (age >= r.path("minAge").asInt(0) && age <= r.path("maxAge").asInt(200));
            if (sexMatch && ageMatch) {
                int score = ("ANY".equalsIgnoreCase(sex) ? 0 : 2) + (r.has("minAge") || r.has("maxAge") ? 1 : 0);
                if (score > best) { selected = r; best = score; }
            }
        }
        if (selected == null) { flag.put("status", "NOT_EVALUATED"); return flag; }
        flag.set("referenceRange", selected.deepCopy());
        if (value.isNumber() && (selected.has("low") || selected.has("high"))) {
            double v = value.asDouble();
            if (selected.has("low") && v < selected.get("low").asDouble()) flag.put("status", "LOW");
            else if (selected.has("high") && v > selected.get("high").asDouble()) flag.put("status", "HIGH");
            else flag.put("status", "NORMAL");
        } else if (selected.has("normalValues") && selected.get("normalValues").isArray()) {
            boolean normal = false;
            for (JsonNode n : selected.get("normalValues")) if (n.asText().equalsIgnoreCase(value.asText())) normal = true;
            flag.put("status", normal ? "NORMAL" : "ABNORMAL");
        } else flag.put("status", "NOT_EVALUATED");
        return flag;
    }

    private ObjectNode applyCalculations(Map<String, JsonNode> defs, ObjectNode values, int age, Gender gender) {
        ObjectNode calculations = objectNode();
        for (Map.Entry<String, JsonNode> e : defs.entrySet()) {
            JsonNode f = e.getValue();
            if (!f.hasNonNull("calculatorKey")) continue;
            String calculator = f.get("calculatorKey").asText();
            Double result = switch (calculator) {
                case "EGFR_CKD_EPI_2021_V1" -> egfr(values, age, gender);
                case "GA_CRL_ROBINSON_FLEMING_V1" -> gaCrl(values);
                case "GA_HADLOCK_BPD_FL_V1" -> gaBpdFl(values);
                case "EFW_HADLOCK_HC_AC_FL_V1" -> efw(values);
                default -> null;
            };
            ObjectNode detail = objectNode();
            detail.put("calculatorKey", calculator);
            if (result == null) detail.put("status", "NOT_CALCULATED");
            else {
                int precision = f.path("precision").asInt(calculator.startsWith("GA_") ? 0 : 2);
                BigDecimal rounded = BigDecimal.valueOf(result).setScale(precision, RoundingMode.HALF_UP);
                values.put(e.getKey(), rounded);
                detail.put("status", "ESTIMATED");
                detail.put("value", rounded);
                if (calculator.startsWith("GA_")) {
                    int days = rounded.intValue();
                    detail.put("display", (days / 7) + " tuần " + (days % 7) + " ngày");
                }
            }
            calculations.set(e.getKey(), detail);
        }
        return calculations;
    }

    private Double egfr(ObjectNode values, int age, Gender gender) {
        if (age < 18 || gender == null || (gender != Gender.MALE && gender != Gender.FEMALE)) return null;
        Double micromol = number(values, "creatinine");
        if (micromol == null || micromol <= 0) return null;
        double scr = micromol / 88.4d;
        boolean female = gender == Gender.FEMALE;
        double k = female ? 0.7 : 0.9;
        double alpha = female ? -0.241 : -0.302;
        return 142d * Math.pow(Math.min(scr / k, 1d), alpha) * Math.pow(Math.max(scr / k, 1d), -1.2d)
                * Math.pow(0.9938d, age) * (female ? 1.012d : 1d);
    }

    private Double gaCrl(ObjectNode values) {
        Double crl = number(values, "crl");
        return crl != null && crl >= 5 && crl <= 85 ? 8.052d * Math.sqrt(crl) + 23.73d : null;
    }

    private Double gaBpdFl(ObjectNode values) {
        Double crl = number(values, "crl");
        if (crl != null && crl >= 5 && crl <= 85) return null;
        Double bpd = number(values, "bpd"), fl = number(values, "fl");
        if (bpd == null || fl == null || bpd <= 0 || fl <= 0) return null;
        double days = 7d * (10.50d + 0.00197d * bpd * fl + 0.095d * fl + 0.073d * bpd);
        return days >= 98 && days <= 238 ? days : null;
    }

    private Double efw(ObjectNode values) {
        Double hcMm = number(values, "hc"), acMm = number(values, "ac"), flMm = number(values, "fl");
        if (hcMm == null || acMm == null || flMm == null || hcMm <= 0 || acMm <= 0 || flMm <= 0) return null;
        double hc = hcMm / 10d, ac = acMm / 10d, fl = flMm / 10d;
        double log = 1.326d - 0.00326d * ac * fl + 0.0107d * hc + 0.0438d * ac + 0.158d * fl;
        return Math.pow(10d, log);
    }

    private Double number(ObjectNode values, String key) {
        JsonNode n = values.get(key);
        return n != null && n.isNumber() ? n.asDouble() : null;
    }

    private List<JsonNode> fields(JsonNode schema) {
        List<JsonNode> result = new ArrayList<>();
        if (schema.has("fields") && schema.get("fields").isArray()) schema.get("fields").forEach(result::add);
        if (schema.has("sections") && schema.get("sections").isArray())
            schema.get("sections").forEach(s -> { if (s.has("fields") && s.get("fields").isArray()) s.get("fields").forEach(result::add); });
        return result;
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText().trim() : "";
    }
}
