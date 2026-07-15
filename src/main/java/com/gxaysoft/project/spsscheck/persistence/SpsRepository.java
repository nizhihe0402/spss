package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.engine.model.*;
import com.gxaysoft.project.spsscheck.model.*;

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

    public void insertRule(long scriptId, int sortNo, Rule rule) throws SQLException {
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
            String label = rule.getDescription() != null ? rule.getDescription() : rule.getTarget();
            ps.setLong(1, scriptId);
            ps.setString(2, ruleCode);
            ps.setString(3, label);
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
            String spssSource = rule.getSpssSource() != null ? rule.getSpssSource() : "";
            ps.setString(16, spssSource.length() > 65535 ? spssSource.substring(0, 65535) : spssSource);
            ps.setString(17, buildRuleJson(rule));
            ps.setString(18, rule.getJavaPreview() != null ? rule.getJavaPreview() : "");
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

    private void insertRuleSteps(long ruleId, Rule rule) throws SQLException {
        List<Step> steps = rule.getSteps();
        if (steps.isEmpty()) {
            // Single-step compute: create one synthetic step
            insertStep(ruleId, 1, "COMPUTE", null, null, rule.getTarget(),
                    rule.getExpression() != null ? rule.getExpression() : "", null, null);
            return;
        }
        int stepNo = 0;
        for (Step step : steps) {
            stepNo++;
            if (step == null) continue;
            String condition = step.getCondition();
            StepAction action = step.getAction();
            if (action == null) continue;

            if (action instanceof ComputeAction) {
                ComputeAction ca = (ComputeAction) action;
                insertStep(ruleId, stepNo, "COMPUTE", condition, null,
                        ca.target(), ca.getExpression(), null, null);
            } else if (action instanceof RecodeAction) {
                RecodeAction ra = (RecodeAction) action;
                String recodeJson = buildRecodeJson(ra);
                insertStep(ruleId, stepNo, "RECODE", condition, ra.getSource(),
                        null, null, null, recodeJson);
            } else if (action instanceof IfAssignAction) {
                IfAssignAction ia = (IfAssignAction) action;
                insertStep(ruleId, stepNo, "IF_ASSIGN", condition, null,
                        ia.target(), null, ia.getValue(), null);
            }
        }
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

    private String buildRuleJson(Rule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(rule.isCheckRule() ? "ROW_CHECK" : "COMPUTE").append("\"");
        sb.append(",\"target\":\"").append(jsonEscape(rule.getTarget())).append("\"");
        String expression = rule.getExpression();
        if (expression != null && !expression.isEmpty()) {
            sb.append(",\"expression\":\"").append(jsonEscape(expression)).append("\"");
        }
        if (!rule.getSteps().isEmpty()) {
            sb.append(",\"steps\":[");
            boolean first = true;
            for (Step step : rule.getSteps()) {
                if (step == null) continue;
                if (!first) sb.append(",");
                sb.append(stepToJson(step));
                first = false;
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String stepToJson(Step step) {
        if (step == null) return "{}";
        StringBuilder sb = new StringBuilder();
        String condition = step.getCondition();
        StepAction action = step.getAction();

        if (action instanceof ComputeAction) {
            ComputeAction ca = (ComputeAction) action;
            sb.append("{\"type\":\"COMPUTE\",\"target\":\"").append(jsonEscape(ca.target()))
                    .append("\",\"expression\":\"").append(jsonEscape(ca.getExpression())).append("\"");
            if (condition != null) {
                sb.append(",\"condition\":\"").append(jsonEscape(condition)).append("\"");
            }
            sb.append("}");
        } else if (action instanceof RecodeAction) {
            RecodeAction ra = (RecodeAction) action;
            sb.append("{\"type\":\"RECODE\",\"source\":\"").append(jsonEscape(ra.getSource()))
                    .append("\",\"target\":\"").append(jsonEscape(ra.target())).append("\"");
            if (condition != null) {
                sb.append(",\"condition\":\"").append(jsonEscape(condition)).append("\"");
            }
            sb.append("}");
        } else if (action instanceof IfAssignAction) {
            IfAssignAction ia = (IfAssignAction) action;
            sb.append("{\"type\":\"IF_ASSIGN\",\"target\":\"").append(jsonEscape(ia.target()))
                    .append("\",\"value\":\"").append(jsonEscape(ia.getValue())).append("\"");
            if (condition != null) {
                sb.append(",\"condition\":\"").append(jsonEscape(condition)).append("\"");
            }
            sb.append("}");
        } else {
            sb.append("{}");
        }
        return sb.toString();
    }

    private String buildRecodeJson(RecodeAction ra) {
        StringBuilder sb = new StringBuilder("{\"type\":\"RECODE\",\"source\":\"");
        sb.append(jsonEscape(ra.getSource()));
        sb.append("\",\"target\":\"");
        sb.append(jsonEscape(ra.target()));
        sb.append("\",\"cases\":[");
        sb.append("]}");
        return sb.toString();
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
