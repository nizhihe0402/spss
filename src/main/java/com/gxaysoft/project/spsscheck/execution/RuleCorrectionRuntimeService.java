package com.gxaysoft.project.spsscheck.execution;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.io.StudentInfoEnricher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleCorrectionRuntimeService {
    private final JdbcTemplate jdbc;

    public RuleCorrectionRuntimeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CorrectionResult apply(long scriptId,
                                  long tableId,
                                  List<RowContext> rows,
                                  Map<String, Map<String, String>> studentInfo) {
        return applyInMemory(rows, loadPlans(scriptId), tableId, studentInfo);
    }

    public List<CorrectionPlan> loadPlans(long scriptId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT correction_enabled, correction_type, correction_variables, " +
                        "correction_write_clean, correction_write_source " +
                        "FROM sps_rule WHERE script_id=? AND correction_enabled=1 ORDER BY sort_no",
                scriptId);
        List<CorrectionPlan> plans = new ArrayList<CorrectionPlan>();
        for (Map<String, Object> row : rows) {
            plans.add(new CorrectionPlan(
                    asInt(row.get("correction_enabled")) == 1,
                    stringValue(row.get("correction_type")),
                    stringValue(row.get("correction_variables")),
                    asInt(row.get("correction_write_clean")) == 1,
                    asInt(row.get("correction_write_source")) == 1));
        }
        return plans;
    }

    public static CorrectionResult applyInMemory(List<RowContext> rows,
                                                 List<CorrectionPlan> plans,
                                                 long tableId,
                                                 Map<String, Map<String, String>> studentInfo) {
        CorrectionResult result = new CorrectionResult();
        if (rows == null || rows.isEmpty() || plans == null || plans.isEmpty()) {
            return result;
        }
        for (CorrectionPlan plan : plans) {
            if (plan == null || !plan.enabled) continue;
            if ("NORMALIZE_REGION_CODE".equals(plan.type)) {
                normalizeRegionCodes(rows, plan, result);
            } else if ("FILL_SCHOOL_CODE".equals(plan.type)) {
                fillSchoolCode(rows, plan, studentInfo, result);
            } else if ("FILL_DOCUMENT_INFO".equals(plan.type)) {
                fillDocumentInfo(rows, plan, studentInfo, result);
            }
        }
        return result;
    }

    private static void normalizeRegionCodes(List<RowContext> rows,
                                             CorrectionPlan plan,
                                             CorrectionResult result) {
        for (RowContext row : rows) {
            for (String variable : plan.variableList()) {
                Object raw = row.get(variable);
                String normalized = rightTwoDigits(raw);
                if (normalized == null) continue;
                String old = raw == null ? "" : String.valueOf(raw).trim();
                if (!normalized.equals(old)) {
                    row.put(variable, normalized);
                    result.put(row.getSampleKey(), variable, normalized, plan.writeClean);
                }
            }
        }
    }

    private static void fillSchoolCode(List<RowContext> rows,
                                       CorrectionPlan plan,
                                       Map<String, Map<String, String>> studentInfo,
                                       CorrectionResult result) {
        if (studentInfo == null || studentInfo.isEmpty()) return;
        for (RowContext row : rows) {
            Object raw = row.get("SCHOOL");
            if (!isBlank(raw)) continue;
            Map<String, String> info = studentInfo.get(row.getSampleKey());
            String school = info == null ? "" : info.get("SCHOOL");
            if (isBlank(school)) continue;
            row.put("SCHOOL", school.trim());
            result.put(row.getSampleKey(), "SCHOOL", school.trim(), plan.writeClean);
        }
    }

    private static void fillDocumentInfo(List<RowContext> rows,
                                         CorrectionPlan plan,
                                         Map<String, Map<String, String>> studentInfo,
                                         CorrectionResult result) {
        if (studentInfo == null || studentInfo.isEmpty()) return;
        for (RowContext row : rows) {
            Map<String, String> info = studentInfo.get(row.getSampleKey());
            if (info == null || info.isEmpty()) continue;
            for (String variable : plan.variableList()) {
                if (!"ZJTYPE".equalsIgnoreCase(variable) && !StudentInfoEnricher.isDocumentNumberVariable(variable)) {
                    continue;
                }
                Object raw = row.get(variable);
                if (!isBlank(raw)) continue;
                String value = info.get(variable.toUpperCase());
                if (isBlank(value)) continue;
                row.put(variable, value.trim());
                result.put(row.getSampleKey(), variable, value.trim(), plan.writeClean);
            }
        }
    }

    private static String rightTwoDigits(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        if (!text.matches("\\d+")) return null;
        if (text.length() == 0) return null;
        if (text.length() <= 2) return leftPadTwo(text);
        return text.substring(text.length() - 2);
    }

    private static String leftPadTwo(String text) {
        return text.length() == 1 ? "0" + text : text;
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int asInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static class CorrectionPlan {
        public final boolean enabled;
        public final String type;
        public final String variables;
        public final boolean writeClean;
        public final boolean writeSource;

        public CorrectionPlan(boolean enabled, String type, String variables, boolean writeClean, boolean writeSource) {
            this.enabled = enabled;
            this.type = type == null ? "" : type.trim();
            this.variables = variables == null ? "" : variables.trim();
            this.writeClean = writeClean;
            this.writeSource = writeSource;
        }

        List<String> variableList() {
            List<String> result = new ArrayList<String>();
            for (String part : variables.split(",")) {
                String value = part == null ? "" : part.trim();
                if (!value.isEmpty()) result.add(value);
            }
            return result;
        }
    }

    public static class CorrectionResult {
        private final Map<String, Map<String, String>> cleanValues = new LinkedHashMap<String, Map<String, String>>();

        void put(String sampleKey, String variable, String value, boolean writeClean) {
            if (!writeClean || sampleKey == null || variable == null) return;
            Map<String, String> row = cleanValues.get(sampleKey);
            if (row == null) {
                row = new LinkedHashMap<String, String>();
                cleanValues.put(sampleKey, row);
            }
            row.put(variable.trim().toUpperCase(), value);
        }

        public String valueFor(String sampleKey, String variable) {
            Map<String, String> row = cleanValues.get(sampleKey);
            return row == null ? null : row.get(variable.trim().toUpperCase());
        }

        public Map<String, Map<String, String>> getCleanValues() {
            return cleanValues;
        }
    }
}
