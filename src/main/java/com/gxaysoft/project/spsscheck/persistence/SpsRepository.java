package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.v1.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SpsRepository {
    private final Connection conn;

    public SpsRepository(Connection conn) {
        this.conn = conn;
    }

    // ── sps_script ──────────────────────────────────────────────

    public long insertScript(String scriptName, String spsText, String tableCode, long tableId) throws SQLException {
        String sql = "INSERT INTO sps_script (script_name, script_content, table_code, table_id, parse_status, version_no, status) " +
                "VALUES (?, ?, ?, ?, 'PARSED', 1, 'DRAFT')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, scriptName);
            ps.setString(2, spsText);
            ps.setString(3, tableCode);
            ps.setLong(4, tableId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // ── sps_rule ────────────────────────────────────────────────

    public void insertRule(long scriptId, int sortNo, SpssCheckRule rule) throws SQLException {
        String sources = String.join(",", rule.getSourceVariables());
        RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                rule.isCheckRule() ? "ROW_CHECK" : "COMPUTE",
                rule.getTarget(), sources, rule.getDescription());
        String sql = "INSERT INTO sps_rule (script_id, rule_code, rule_name, rule_type, target_variable, " +
                "source_variables, correction_enabled, correction_type, correction_variables, correction_source, " +
                "correction_strategy, correction_apply_stage, correction_write_clean, correction_write_source, " +
                "correction_description, spss_source, rule_json, java_preview, sort_no, affect_clean, warning_message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String ruleCode = String.format("R%03d", sortNo);
            ps.setLong(1, scriptId);
            ps.setString(2, ruleCode);
            ps.setString(3, rule.getLabel());
            ps.setString(4, rule.isCheckRule() ? "ROW_CHECK" : "COMPUTE");
            ps.setString(5, rule.getTarget());
            ps.setString(6, sources);
            ps.setInt(7, correction.enabled ? 1 : 0);
            ps.setString(8, correction.type);
            ps.setString(9, correction.variables);
            ps.setString(10, correction.source);
            ps.setString(11, correction.strategy);
            ps.setString(12, correction.applyStage);
            ps.setInt(13, correction.writeClean ? 1 : 0);
            ps.setInt(14, correction.writeSource ? 1 : 0);
            ps.setString(15, correction.description);
            ps.setString(16, rule.getSpssSource().length() > 65535
                    ? rule.getSpssSource().substring(0, 65535) : rule.getSpssSource());
            ps.setString(17, buildRuleJson(rule));
            ps.setString(18, rule.getJavaRule());
            ps.setInt(19, sortNo);
            ps.setInt(20, rule.isCheckRule() ? 1 : 0);
            ps.setString(21, rule.getDescription());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                long ruleId = rs.getLong(1);
                insertRuleSteps(ruleId, rule);
            }
        }
    }

    public void insertScriptQuestionMappings(long scriptId, List<QuestionMapping> mappings) throws SQLException {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO sps_script_question_mapping " +
                "(script_id, variable_name, question_id, question_content, source_table_id, export_content, sort_no) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int sortNo = 0;
            for (QuestionMapping mapping : mappings) {
                if (mapping == null || mapping.getQuestionId() <= 0) continue;
                sortNo++;
                ps.setLong(1, scriptId);
                ps.setString(2, mapping.getVariableNameOriginal());
                ps.setLong(3, mapping.getQuestionId());
                ps.setString(4, mapping.getContent());
                ps.setLong(5, mapping.getTableId());
                ps.setString(6, mapping.getVariableNameOriginal());
                ps.setInt(7, sortNo);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertRuleSteps(long ruleId, SpssCheckRule rule) throws SQLException {
        List<RuleStep> steps = rule.getSteps();
        if (steps.isEmpty()) {
            // Single-step compute: create one synthetic step
            insertStep(ruleId, 1, "COMPUTE", null, null, rule.getTarget(), rule.getExpression(), null, null);
            return;
        }
        int stepNo = 0;
        for (RuleStep step : steps) {
            stepNo++;
            insertStepFrom(ruleId, stepNo, step);
        }
    }

    private void insertStepFrom(long ruleId, int stepNo, RuleStep step) throws SQLException {
        if (step instanceof ConditionalRuleStep) {
            ConditionalRuleStep cs = (ConditionalRuleStep) step;
            // First insert the condition wrapper, then recurse into delegate
            insertConditionalStep(ruleId, stepNo, cs);
        } else if (step instanceof ComputeRuleStep) {
            ComputeRuleStep cs = (ComputeRuleStep) step;
            insertStep(ruleId, stepNo, "COMPUTE", null, null, cs.getTarget(), cs.getExpression(), null, null);
        } else if (step instanceof RecodeRuleStep) {
            RecodeRuleStep rs = (RecodeRuleStep) step;
            String recodeJson = buildRecodeJson(rs);
            insertStep(ruleId, stepNo, "RECODE", null, rs.sourceVariables().isEmpty() ? "" : rs.sourceVariables().get(0),
                    null, null, null, recodeJson);
        } else if (step instanceof IfAssignRuleStep) {
            IfAssignRuleStep is = (IfAssignRuleStep) step;
            insertStep(ruleId, stepNo, "IF_ASSIGN", null, null, null, null, null, null);
        }
    }

    private void insertConditionalStep(long ruleId, int stepNo, ConditionalRuleStep cs) throws SQLException {
        // Insert the condition wrapper
        insertStep(ruleId, stepNo, "CONDITIONAL", extractConditionText(cs), null, null, null, null, null);
        // Insert the delegate as stepNo+1
        insertStepFrom(ruleId, stepNo + 1, getDelegate(cs));
    }

    private void insertStep(long ruleId, int stepNo, String stepType, String condition,
                            String sourceVar, String targetVar, String expression,
                            String assignValue, String recodeJson) throws SQLException {
        String sql = "INSERT INTO sps_rule_step (rule_id, step_no, step_type, condition_text, " +
                "source_variable, target_variable, expression_text, assign_value, recode_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ruleId);
            ps.setInt(2, stepNo);
            ps.setString(3, stepType);
            ps.setString(4, condition);
            ps.setString(5, sourceVar);
            ps.setString(6, targetVar);
            ps.setString(7, expression);
            ps.setString(8, assignValue);
            ps.setString(9, recodeJson);
            ps.executeUpdate();
        }
    }

    // ── sps_output_rule ─────────────────────────────────────────

    public void insertOutputRule(long scriptId, int sortNo, SpssOutputRule rule) throws SQLException {
        String sql = "INSERT INTO sps_output_rule (script_id, output_code, output_name, output_type, " +
                "select_condition, spss_source, java_preview, sort_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, scriptId);
            ps.setString(2, String.format("O%03d", sortNo));
            ps.setString(3, rule.getSheetName());
            ps.setString(4, rule.getSheetName().contains("清理后") ? "CLEAN_DATA" : "ERROR_GROUP");
            ps.setString(5, rule.getCondition());
            ps.setString(6, rule.getSpssSource());
            ps.setString(7, rule.getJavaRule());
            ps.setInt(8, sortNo);
            ps.executeUpdate();
        }
    }

    // ── sps_unsupported_statement ───────────────────────────────

    public void insertUnsupportedStatement(long scriptId, String stmtType, String reason, String riskLevel) throws SQLException {
        String sql = "INSERT INTO sps_unsupported_statement (script_id, statement_type, reason, risk_level) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, scriptId);
            ps.setString(2, stmtType);
            ps.setString(3, reason);
            ps.setString(4, riskLevel);
            ps.executeUpdate();
        }
    }

    // ── JSON builders ───────────────────────────────────────────

    private String buildRuleJson(SpssCheckRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(rule.isCheckRule() ? "ROW_CHECK" : "COMPUTE").append("\"");
        sb.append(",\"target\":\"").append(jsonEscape(rule.getTarget())).append("\"");
        if (!rule.getExpression().isEmpty()) {
            sb.append(",\"expression\":\"").append(jsonEscape(rule.getExpression())).append("\"");
        }
        if (!rule.getSteps().isEmpty()) {
            sb.append(",\"steps\":[");
            boolean first = true;
            for (RuleStep step : rule.getSteps()) {
                if (!first) sb.append(",");
                sb.append(stepToJson(step));
                first = false;
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String stepToJson(RuleStep step) {
        if (step instanceof ConditionalRuleStep) {
            ConditionalRuleStep cs = (ConditionalRuleStep) step;
            return "{\"type\":\"CONDITIONAL\",\"condition\":\"" + jsonEscape(extractConditionText(cs))
                    + "\",\"step\":" + stepToJson(getDelegate(cs)) + "}";
        }
        if (step instanceof ComputeRuleStep) {
            ComputeRuleStep cs = (ComputeRuleStep) step;
            return "{\"type\":\"COMPUTE\",\"target\":\"" + jsonEscape(cs.getTarget())
                    + "\",\"expression\":\"" + jsonEscape(cs.getExpression()) + "\"}";
        }
        if (step instanceof RecodeRuleStep) {
            return buildRecodeJson((RecodeRuleStep) step);
        }
        if (step instanceof IfAssignRuleStep) {
            return "{\"type\":\"IF_ASSIGN\"}";
        }
        return "{}";
    }

    private String buildRecodeJson(RecodeRuleStep rs) {
        StringBuilder sb = new StringBuilder("{\"type\":\"RECODE\",\"source\":\"");
        List<String> sv = rs.sourceVariables();
        sb.append(sv.isEmpty() ? "" : jsonEscape(sv.get(0)));
        sb.append("\",\"cases\":[");
        // Cases are embedded in the step — just record it's a recode step
        sb.append("]}");
        return sb.toString();
    }

    private String extractConditionText(ConditionalRuleStep cs) {
        String jr = cs.javaRule();
        int start = jr.indexOf("\"") + 1;
        int end = jr.indexOf("\"", start);
        return end > start ? jr.substring(start, end) : "";
    }

    private RuleStep getDelegate(ConditionalRuleStep cs) {
        try {
            java.lang.reflect.Field f = ConditionalRuleStep.class.getDeclaredField("delegate");
            f.setAccessible(true);
            return (RuleStep) f.get(cs);
        } catch (Exception e) {
            return null;
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    // ── Collect unsupported statements from SPS text ────────────

    public static List<String[]> collectUnsupported(String spsText) {
        List<String[]> result = new ArrayList<>();
        String[] keywords = {
                "DESCRIPTIVES", "FREQUENCIES", "CTABLES", "DATASET COPY", "DATASET ACTIVATE",
                "FILTER OFF", "USE ALL", "SPLIT FILE"
        };
        for (String kw : keywords) {
            if (spsText.toUpperCase().contains(kw.toUpperCase())) {
                result.add(new String[]{kw, "SPSS statistical/utility command not executed by engine", "MEDIUM"});
            }
        }
        return result;
    }
}
