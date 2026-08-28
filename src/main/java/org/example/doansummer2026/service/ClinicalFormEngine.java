package org.example.doansummer2026.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ClinicalFormEngine {
    private static final Set<String> TYPES = Set.of("TEXT", "TEXTAREA", "NUMBER", "DATE", "SELECT", "BOOLEAN");
    private static final Set<String> CALCULATORS = Set.of(
            "EGFR_CKD_EPI_2021_V1", "GA_CRL_ROBINSON_FLEMING_V1",
            "GA_HADLOCK_BPD_FL_V1", "EFW_HADLOCK_HC_AC_FL_V1",
            "NON_HDL_C_V1", "INDIRECT_BILIRUBIN_V1", "BUN_FROM_UREA_V1");

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
            if (field.hasNonNull("pattern")) try {
                java.util.regex.Pattern.compile(field.get("pattern").asText());
            } catch (java.util.regex.PatternSyntaxException ex) {
                throw new BadRequestException("Pattern không hợp lệ cho " + key);
            }
            validateCondition(field.get("visibleWhen"), keys, key);
            validateCondition(field.get("requiredWhen"), keys, key);
        }
    }

    private void validateCondition(JsonNode condition, Set<String> precedingKeys, String currentKey) {
        if (condition == null || condition.isNull()) return;
        if (!condition.isObject() || !condition.hasNonNull("field"))
            throw new BadRequestException("Điều kiện của " + currentKey + " không hợp lệ");
        if (!precedingKeys.contains(condition.get("field").asText()))
            throw new BadRequestException("Điều kiện của " + currentKey + " phải tham chiếu trường đứng trước");
    }

    /** Giữ hành vi cũ cho phiếu khám và các nơi đã tích hợp trước đây. */
    public JsonNode validateAndEnrich(JsonNode schema, JsonNode input, LocalDate birthDate,
                                      Gender gender, LocalDate performedDate) {
        return validateAndEnrich(schema, input, birthDate, gender, performedDate, true);
    }

    /** @param requireComplete false khi lưu nháp; true khi ký/hoàn tất. */
    public JsonNode validateAndEnrich(JsonNode schema, JsonNode input, LocalDate birthDate,
                                      Gender gender, LocalDate performedDate, boolean requireComplete) {
        validateSchema(schema);
        if (input == null || input.isNull()) input = objectNode();
        if (!input.isObject()) throw new BadRequestException("Dữ liệu form phải là JSON object");
        ObjectNode output = ((ObjectNode) input).deepCopy();
        output.remove("_meta");

        Map<String, JsonNode> definitions = new LinkedHashMap<>();
        for (JsonNode field : fields(schema)) definitions.put(field.get("key").asText(), field);
        output.propertyNames().forEach(key -> {
            if (!definitions.containsKey(key)) throw new BadRequestException("Trường không thuộc template: " + key);
        });

        // Giá trị tự tính luôn do backend tạo lại.
        definitions.forEach((key, field) -> {
            if (field.hasNonNull("calculatorKey")) output.remove(key);
        });

        for (Map.Entry<String, JsonNode> entry : definitions.entrySet()) {
            String key = entry.getKey();
            JsonNode field = entry.getValue();
            if (!conditionMatches(field.get("visibleWhen"), output)) {
                output.remove(key);
                continue;
            }
            JsonNode value = output.get(key);
            boolean conditionalRequired = field.hasNonNull("requiredWhen")
                    && conditionMatches(field.get("requiredWhen"), output);
            boolean required = field.path("required").asBoolean(false)
                    || field.path("requiredOnSign").asBoolean(false)
                    || conditionalRequired;
            if (requireComplete && required && isEmpty(value) && !field.hasNonNull("calculatorKey"))
                throw new BadRequestException("Vui lòng nhập " + field.path("label").asText(key));
            if (!isEmpty(value)) validateValue(field, value);
        }

        ObjectNode calculations = applyCalculations(definitions, output, ageYears(birthDate, performedDate), gender);
        JsonNode warnings = validateRules(schema.path("rules"), output, requireComplete);
        ObjectNode flags = objectNode();
        for (Map.Entry<String, JsonNode> entry : definitions.entrySet()) {
            String key = entry.getKey();
            JsonNode field = entry.getValue();
            JsonNode value = output.get(key);
            if (isEmpty(value)) {
                if (field.hasNonNull("calculatorKey")) {
                    ObjectNode flag = objectNode();
                    flag.put("status", calculations.path(key).path("status").asText("NOT_CALCULATED"));
                    flags.set(key, flag);
                }
                continue;
            }
            if (field.has("referenceRanges")) {
                flags.set(key, evaluateRange(field, value, birthDate, performedDate, gender, output));
            } else if (field.hasNonNull("calculatorKey")) {
                ObjectNode flag = objectNode();
                flag.put("status", calculations.path(key).path("status").asText("NOT_CALCULATED"));
                flags.set(key, flag);
            }
        }

        ObjectNode meta = objectNode();
        meta.set("flags", flags);
        meta.set("calculations", calculations);
        meta.set("warnings", warnings);
        output.set("_meta", meta);
        return output;
    }

    private void validateValue(JsonNode field, JsonNode value) {
        String key = field.get("key").asText();
        String label = field.path("label").asText(key);
        String type = field.get("type").asText().toUpperCase(Locale.ROOT);
        if ("NUMBER".equals(type)) {
            if (!value.isNumber()) throw new BadRequestException(label + " phải là số");
            double v = value.asDouble();
            if (!Double.isFinite(v)) throw new BadRequestException(label + " không hợp lệ");
            if (field.has("min") && v < field.get("min").asDouble()) throw new BadRequestException(label + " nhỏ hơn giới hạn nhập");
            if (field.has("max") && v > field.get("max").asDouble()) throw new BadRequestException(label + " vượt giới hạn nhập");
        } else if ("BOOLEAN".equals(type) && !value.isBoolean()) {
            throw new BadRequestException(label + " phải là true/false");
        } else if (("TEXT".equals(type) || "TEXTAREA".equals(type) || "DATE".equals(type) || "SELECT".equals(type)) && !value.isTextual()) {
            throw new BadRequestException(label + " phải là chuỗi");
        }
        if ("SELECT".equals(type)) {
            boolean allowed = false;
            for (JsonNode option : field.get("options")) {
                String candidate = option.isTextual() ? option.asText() : option.path("value").asText();
                if (candidate.equals(value.asText())) { allowed = true; break; }
            }
            if (!allowed) throw new BadRequestException("Giá trị không hợp lệ cho " + label);
        }
        if ("DATE".equals(type)) try { LocalDate.parse(value.asText()); }
        catch (Exception ex) { throw new BadRequestException(label + " phải theo định dạng YYYY-MM-DD"); }
        if (value.isTextual() && field.hasNonNull("pattern")
                && !value.asText().matches(field.get("pattern").asText())) {
            throw new BadRequestException(field.path("patternMessage").asText(label + " không đúng định dạng"));
        }
    }

    private ObjectNode evaluateRange(JsonNode field, JsonNode value, LocalDate birthDate,
                                     LocalDate performedDate, Gender gender, ObjectNode values) {
        ObjectNode flag = objectNode();
        JsonNode selected = selectRange(field.get("referenceRanges"), birthDate, performedDate, gender, values);
        if (selected == null) { flag.put("status", "NOT_EVALUATED"); return flag; }
        flag.set("referenceRange", selected.deepCopy());
        String status = "NOT_EVALUATED";
        if (value.isNumber() && (selected.has("low") || selected.has("high"))) {
            double v = value.asDouble();
            String qualifier = field.hasNonNull("qualifierKey")
                    ? values.path(field.get("qualifierKey").asText()).asText("EQUAL") : "EQUAL";
            if ("LESS_THAN".equals(qualifier)) {
                if (!selected.has("low") && selected.has("high") && v <= selected.get("high").asDouble()) status = "NORMAL";
            } else if ("GREATER_THAN".equals(qualifier)) {
                if (selected.has("high") && v >= selected.get("high").asDouble()) status = "HIGH";
            } else if (selected.has("low") && v < selected.get("low").asDouble()) status = "LOW";
            else if (selected.has("high") && v > selected.get("high").asDouble()) status = "HIGH";
            else status = "NORMAL";

            JsonNode critical = selectRange(field.get("criticalRanges"), birthDate, performedDate, gender, values);
            if (critical != null) {
                if (critical.has("low") && v < critical.get("low").asDouble()) status = "CRITICAL_LOW";
                if (critical.has("high") && v > critical.get("high").asDouble()) status = "CRITICAL_HIGH";
                flag.set("criticalRange", critical.deepCopy());
            }
        } else if (selected.has("normalValues") && selected.get("normalValues").isArray()) {
            boolean normal = false;
            for (JsonNode normalValue : selected.get("normalValues"))
                if (normalValue.asText().equalsIgnoreCase(value.asText())) normal = true;
            status = normal ? "NORMAL" : "ABNORMAL";
        }
        flag.put("status", status);
        return flag;
    }

    private JsonNode selectRange(JsonNode ranges, LocalDate birthDate, LocalDate performedDate,
                                 Gender gender, ObjectNode values) {
        if (ranges == null || !ranges.isArray()) return null;
        JsonNode selected = null;
        int best = Integer.MIN_VALUE;
        for (JsonNode range : ranges) {
            if (!conditionMatches(range.get("when"), values)) continue;
            String sex = range.path("sex").asText("ANY");
            if (!("ANY".equalsIgnoreCase(sex) || (gender != null && sex.equalsIgnoreCase(gender.name())))) continue;
            long age = ageInUnit(birthDate, performedDate, range.path("ageUnit").asText("YEARS"));
            boolean ageConstrained = range.has("minAge") || range.has("maxAge");
            if (ageConstrained && age < 0) continue;
            if (ageConstrained && (age < range.path("minAge").asLong(0)
                    || age > range.path("maxAge").asLong(Long.MAX_VALUE))) continue;
            int score = "ANY".equalsIgnoreCase(sex) ? 0 : 4;
            if (range.has("minAge") || range.has("maxAge")) score += 2;
            if (range.has("when")) score += 8;
            if (score > best) { selected = range; best = score; }
        }
        return selected;
    }

    private ObjectNode applyCalculations(Map<String, JsonNode> definitions, ObjectNode values, int age, Gender gender) {
        ObjectNode calculations = objectNode();
        for (Map.Entry<String, JsonNode> entry : definitions.entrySet()) {
            JsonNode field = entry.getValue();
            if (!field.hasNonNull("calculatorKey")) continue;
            String calculator = field.get("calculatorKey").asText();
            Double result = switch (calculator) {
                case "EGFR_CKD_EPI_2021_V1" -> egfr(values, age, gender);
                case "GA_CRL_ROBINSON_FLEMING_V1" -> gaCrl(values);
                case "GA_HADLOCK_BPD_FL_V1" -> gaBpdFl(values);
                case "EFW_HADLOCK_HC_AC_FL_V1" -> efw(values);
                case "NON_HDL_C_V1" -> subtract(values, "totalCholesterol", "hdlC");
                case "INDIRECT_BILIRUBIN_V1" -> nonNegativeSubtract(values, "bilirubinTotal", "bilirubinDirect");
                case "BUN_FROM_UREA_V1" -> multiply(values, "urea", 2.801d);
                default -> null;
            };
            ObjectNode detail = objectNode();
            detail.put("calculatorKey", calculator);
            if (result == null || !Double.isFinite(result)) detail.put("status", "NOT_CALCULATED");
            else {
                int precision = field.path("precision").asInt(calculator.startsWith("GA_") ? 0 : 2);
                BigDecimal rounded = BigDecimal.valueOf(result).setScale(precision, RoundingMode.HALF_UP);
                values.put(entry.getKey(), rounded);
                detail.put("status", "ESTIMATED");
                detail.put("value", rounded);
                if (calculator.startsWith("GA_")) {
                    int days = rounded.intValue();
                    detail.put("display", (days / 7) + " tuần " + (days % 7) + " ngày");
                }
            }
            calculations.set(entry.getKey(), detail);
        }
        return calculations;
    }

    private JsonNode validateRules(JsonNode rules, ObjectNode values, boolean requireComplete) {
        var warnings = JsonNodeFactory.instance.arrayNode();
        if (rules == null || !rules.isArray()) return warnings;
        for (JsonNode rule : rules) {
            if (rule.path("onSignOnly").asBoolean(false) && !requireComplete) continue;
            String type = rule.path("type").asText();
            boolean violated = false;
            if ("SUM_BETWEEN".equals(type) && rule.path("keys").isArray()) {
                double sum = 0; boolean complete = true;
                for (JsonNode key : rule.get("keys")) {
                    Double n = number(values, key.asText());
                    if (n == null) { complete = false; break; }
                    sum += n;
                }
                violated = complete && (sum < rule.path("min").asDouble() || sum > rule.path("max").asDouble());
            } else if ("LESS_THAN_OR_EQUAL".equals(type)) {
                Double left = number(values, rule.path("left").asText());
                Double right = number(values, rule.path("right").asText());
                violated = left != null && right != null && left > right;
            } else if ("ABSOLUTE_FROM_PERCENT".equals(type) && rule.path("pairs").isArray()) {
                Double total = number(values, rule.path("total").asText());
                if (total != null) for (JsonNode pair : rule.get("pairs")) {
                    Double percent = number(values, pair.path("percent").asText());
                    Double absolute = number(values, pair.path("absolute").asText());
                    if (percent == null || absolute == null) continue;
                    double expected = total * percent / 100d;
                    double tolerance = Math.max(0.05d, expected * rule.path("tolerancePercent").asDouble(15d) / 100d);
                    if (Math.abs(absolute - expected) > tolerance) { violated = true; break; }
                }
            } else if ("AT_LEAST_ONE_TRUE".equals(type) && rule.path("keys").isArray()) {
                violated = true;
                for (JsonNode key : rule.get("keys")) {
                    if (values.path(key.asText()).asBoolean(false)) { violated = false; break; }
                }
            } else if ("BOOLEAN_MUST_BE_TRUE_WHEN".equals(type)) {
                boolean applies = conditionMatches(rule.get("when"), values);
                violated = applies && !values.path(rule.path("field").asText()).asBoolean(false);
            } else if ("VALUE_NOT_ALLOWED_WHEN".equals(type)) {
                boolean applies = conditionMatches(rule.get("when"), values);
                JsonNode actual = values.get(rule.path("field").asText());
                violated = applies && actual != null
                        && Objects.equals(normalized(actual), normalized(rule.get("value")));
            }
            if (!violated) continue;
            String message = rule.path("message").asText("Dữ liệu xét nghiệm chưa nhất quán");
            if ("ERROR".equalsIgnoreCase(rule.path("severity").asText("WARNING"))) throw new BadRequestException(message);
            warnings.add(message);
        }
        return warnings;
    }

    private boolean conditionMatches(JsonNode condition, ObjectNode values) {
        if (condition == null || condition.isNull()) return true;
        JsonNode actual = values.get(condition.path("field").asText());
        if (condition.has("equals")) return Objects.equals(normalized(actual), normalized(condition.get("equals")));
        if (condition.has("notEquals")) return !Objects.equals(normalized(actual), normalized(condition.get("notEquals")));
        if (condition.has("in") && condition.get("in").isArray()) {
            for (JsonNode candidate : condition.get("in"))
                if (Objects.equals(normalized(actual), normalized(candidate))) return true;
            return false;
        }
        return actual != null && !actual.isNull();
    }

    private Object normalized(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNumber()) return node.asDouble();
        return node.asText();
    }

    private boolean isEmpty(JsonNode value) {
        return value == null || value.isNull() || (value.isTextual() && value.asText().isBlank());
    }

    private int ageYears(LocalDate birthDate, LocalDate performedDate) {
        return birthDate == null ? -1 : Period.between(birthDate, performedDate == null ? LocalDate.now() : performedDate).getYears();
    }

    private long ageInUnit(LocalDate birthDate, LocalDate performedDate, String unit) {
        if (birthDate == null) return -1;
        LocalDate date = performedDate == null ? LocalDate.now() : performedDate;
        return switch (unit.toUpperCase(Locale.ROOT)) {
            case "DAYS" -> ChronoUnit.DAYS.between(birthDate, date);
            case "MONTHS" -> ChronoUnit.MONTHS.between(birthDate, date);
            default -> Period.between(birthDate, date).getYears();
        };
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

    private Double subtract(ObjectNode values, String left, String right) {
        Double a = number(values, left), b = number(values, right);
        return a == null || b == null ? null : a - b;
    }

    private Double nonNegativeSubtract(ObjectNode values, String left, String right) {
        Double result = subtract(values, left, right);
        return result == null || result < 0 ? null : result;
    }

    private Double multiply(ObjectNode values, String key, double factor) {
        Double value = number(values, key);
        return value == null ? null : value * factor;
    }

    private Double number(ObjectNode values, String key) {
        JsonNode node = values.get(key);
        return node != null && node.isNumber() ? node.asDouble() : null;
    }

    private List<JsonNode> fields(JsonNode schema) {
        List<JsonNode> result = new ArrayList<>();
        if (schema.has("fields") && schema.get("fields").isArray()) schema.get("fields").forEach(result::add);
        if (schema.has("sections") && schema.get("sections").isArray())
            schema.get("sections").forEach(section -> {
                if (section.has("fields") && section.get("fields").isArray()) section.get("fields").forEach(result::add);
            });
        return result;
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText().trim() : "";
    }
}
