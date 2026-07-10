package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.engine.model.OutputRule;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.persistence.RuleCorrectionPlan;
import com.gxaysoft.project.spsscheck.persistence.ScriptService;
import com.gxaysoft.project.spsscheck.persistence.SourceQuestionMappingSyncService;
import com.gxaysoft.project.spsscheck.persistence.SpsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {
    private static final Logger log = LoggerFactory.getLogger(ScriptController.class);

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/{id}")
    public Map<String, Object> getScript(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("content", scriptService.loadScriptContent(id));
        return result;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        try {
            return jdbc.queryForList(
                "SELECT id, script_name AS name, parse_status AS parse, status, " +
                "version_no AS ver, (SELECT COUNT(*) FROM sps_rule WHERE script_id=s.id) AS rules, " +
                "table_id AS tableId, project_id AS projectId, project_type AS projectType, year, " +
                "DATE_FORMAT(created_time, '%Y-%m-%d %H:%i') AS time " +
                "FROM sps_script s ORDER BY id DESC");
        } catch (Exception e) {
            log.warn("sps_script缺少project_id等列，降级查询: {}", e.getMessage());
            return jdbc.queryForList(
                "SELECT id, script_name AS name, parse_status AS parse, status, " +
                "version_no AS ver, (SELECT COUNT(*) FROM sps_rule WHERE script_id=s.id) AS rules, " +
                "table_id AS tableId, " +
                "DATE_FORMAT(created_time, '%Y-%m-%d %H:%i') AS time " +
                "FROM sps_script s ORDER BY id DESC");
        }
    }

    @GetMapping("/{id}/rules")
    public Map<String, Object> getRules(@PathVariable Long id) {
        ParsedScript parsed = scriptService.getParsedRules(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("rules", parsed.getRules());
        result.put("datasetRules", parsed.getDatasetRules());
        result.put("outputRules", parsed.getOutputRules());
        result.put("totalRules", parsed.totalRules());
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        jdbc.update("DELETE FROM sps_rule_step WHERE rule_id IN (SELECT id FROM sps_rule WHERE script_id=?)", id);
        jdbc.update("DELETE FROM sps_rule WHERE script_id=?", id);
        jdbc.update("DELETE FROM sps_output_rule WHERE script_id=?", id);
        jdbc.update("DELETE FROM sps_unsupported_statement WHERE script_id=?", id);
        jdbc.update("DELETE FROM sps_script WHERE id=?", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "已删除");
        return result;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestParam String status) {
        jdbc.update("UPDATE sps_script SET status=? WHERE id=?", status, id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "状态已更新");
        return result;
    }

    /**
     * 重新解析单个脚本 — 删除旧规则重新解析并保存。
     */
    @PostMapping("/{id}/reparse")
    public Map<String, Object> reparse(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String spsText = scriptService.loadScriptContent(id);
            if (spsText.isEmpty()) {
                result.put("code", 1);
                result.put("msg", "脚本内容为空");
                return result;
            }
            // 删除旧规则
            jdbc.update("DELETE FROM sps_rule_step WHERE rule_id IN (SELECT id FROM sps_rule WHERE script_id=?)", id);
            jdbc.update("DELETE FROM sps_rule WHERE script_id=?", id);
            jdbc.update("DELETE FROM sps_output_rule WHERE script_id=?", id);
            jdbc.update("DELETE FROM sps_unsupported_statement WHERE script_id=?", id);

            // 重新解析
            ParsedScript parsed = SpssParser.parse(spsText);
            List<Rule> rules = parsed.getRules();
            List<OutputRule> outputRules = parsed.getOutputRules();

            Long tableId = loadScriptTableId(id);
            int sortNo = 0;
            for (Rule rd : rules) {
                sortNo++;
                insertRule(id, sortNo, rd, tableId);
            }
            int outNo = 0;
            for (OutputRule or : outputRules) {
                outNo++;
                insertOutputRule(id, outNo, or);
            }
            for (String[] stmt : SpsRepository.collectUnsupported(spsText)) {
                jdbc.update("INSERT INTO sps_unsupported_statement (script_id, statement_type, reason, risk_level) VALUES (?,?,?,?)",
                        id, stmt[0], stmt[1], stmt[2]);
            }
            jdbc.update("UPDATE sps_script SET parse_status='PARSED', parse_message=NULL WHERE id=?", id);

            result.put("code", 0);
            result.put("msg", "重新解析完成: " + rules.size() + " 条规则, " + outputRules.size() + " 个输出分组");
            result.put("rules", rules.size());
            result.put("outputRules", outputRules.size());
        } catch (Exception e) {
            log.error("重新解析失败: scriptId={}", id, e);
            jdbc.update("UPDATE sps_script SET parse_status='FAILED', parse_message=? WHERE id=?",
                    truncate(e.getMessage(), 500), id);
            result.put("code", 1);
            result.put("msg", "解析失败: " + e.getMessage());
        }
        return result;
    }

    private void insertRule(long scriptId, int sortNo, Rule rd, Long tableId) {
        String code = String.format("R%03d", sortNo);
        String sources = rd.getSourceVariables() != null ? String.join(",", rd.getSourceVariables()) : "";
        String sourceQuestionMappings = new SourceQuestionMappingSyncService(jdbc).buildForSources(sources, tableId);
        RuleType rt = rd.getType() != null ? rd.getType() : RuleType.CONDITIONAL_BLOCK;
        RuleCorrectionPlan correction = RuleCorrectionPlan.detect(rt.name(), rd.getTarget(), sources, rd.getDescription());
        String spssSource = rd.getSpssSource() != null ? rd.getSpssSource() : "";
        String javaPreview = rd.getJavaPreview() != null ? rd.getJavaPreview() : "";
        String executionChain = rd.getExecutionChain() != null ? rd.getExecutionChain() : "";

        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(conn -> {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sps_rule (script_id, rule_code, rule_name, rule_type, target_variable, " +
                "source_variables, source_question_mappings, correction_enabled, correction_type, correction_variables, " +
                "correction_source, correction_strategy, correction_apply_stage, correction_write_clean, " +
                "correction_write_source, correction_description, spss_source, rule_json, java_preview, " +
                "execution_chain, sort_no, affect_clean, warning_message, start_line, end_line) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, scriptId); ps.setString(2, code); ps.setString(3, rd.getTarget());
            ps.setString(4, rt.name()); ps.setString(5, rd.getTarget()); ps.setString(6, sources);
            ps.setString(7, sourceQuestionMappings);
            ps.setInt(8, correction.enabled ? 1 : 0); ps.setString(9, correction.type);
            ps.setString(10, correction.variables); ps.setString(11, correction.source);
            ps.setString(12, correction.strategy); ps.setString(13, correction.applyStage);
            ps.setInt(14, correction.writeClean ? 1 : 0); ps.setInt(15, correction.writeSource ? 1 : 0);
            ps.setString(16, correction.description); ps.setString(17, truncate(spssSource, 65535));
            ps.setString(18, "{\"v2\":true,\"type\":\"" + rt.name() + "\"}");
            ps.setString(19, javaPreview); ps.setString(20, executionChain);
            ps.setInt(21, sortNo);
            ps.setInt(22, rt == RuleType.IDENTITY_CHECK || rt == RuleType.MISSING_CHECK
                    || rt == RuleType.RANGE_CHECK || rt == RuleType.CONSISTENCY_CHECK
                    || rt == RuleType.DOCUMENT_CHECK ? 1 : 0);
            ps.setString(23, rd.getDescription());
            ps.setInt(24, rd.getStartLine()); ps.setInt(25, rd.getEndLine());
            return ps;
        }, keyHolder);

        long ruleId = keyHolder.getKey().longValue();
        insertRuleSteps(ruleId, rd);
    }

    private void insertRuleSteps(long ruleId, Rule rd) {
        if (rd.getSteps() == null || rd.getSteps().isEmpty()) return;
        int stepNo = 0;
        for (com.gxaysoft.project.spsscheck.engine.model.Step step : rd.getSteps()) {
            stepNo++;
            String stepType = "COMPUTE";
            String sourceVar = null, targetVar = step.getTarget(), exprText = null, assignVal = null, recodeJson = null;
            if (step.getAction() instanceof com.gxaysoft.project.spsscheck.engine.model.ComputeAction) {
                stepType = "COMPUTE";
                exprText = ((com.gxaysoft.project.spsscheck.engine.model.ComputeAction) step.getAction()).getExpression();
            } else if (step.getAction() instanceof com.gxaysoft.project.spsscheck.engine.model.RecodeAction) {
                stepType = "RECODE";
                com.gxaysoft.project.spsscheck.engine.model.RecodeAction ra =
                    (com.gxaysoft.project.spsscheck.engine.model.RecodeAction) step.getAction();
                sourceVar = ra.getSource();
                recodeJson = recodeCasesToJson(ra.getCases());
            } else if (step.getAction() instanceof com.gxaysoft.project.spsscheck.engine.model.IfAssignAction) {
                stepType = "IF_ASSIGN";
                assignVal = ((com.gxaysoft.project.spsscheck.engine.model.IfAssignAction) step.getAction()).getValue();
            }
            jdbc.update(
                "INSERT INTO sps_rule_step (rule_id, step_no, step_type, condition_text, " +
                "source_variable, target_variable, expression_text, assign_value, recode_json) " +
                "VALUES (?,?,?,?,?,?,?,?,?)",
                ruleId, stepNo, stepType, step.getCondition(),
                sourceVar, targetVar, exprText, assignVal, recodeJson);
        }
    }

    private String recodeCasesToJson(java.util.List<com.gxaysoft.project.spsscheck.model.RecodeCase> cases) {
        if (cases == null || cases.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cases.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(cases.get(i).toDisplayString().replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private void insertOutputRule(long scriptId, int sortNo, OutputRule or) {
        String code = String.format("O%03d", sortNo);
        String type = or.getSheetName() != null && or.getSheetName().contains("清理后") ? "CLEAN_DATA" : "ERROR_GROUP";
        jdbc.update("INSERT INTO sps_output_rule (script_id, output_code, output_name, output_type, " +
                "select_condition, spss_source, java_preview, sort_no) VALUES (?,?,?,?,?,?,?,?)",
                scriptId, code, or.getSheetName(), type,
                or.getCondition(), or.getSpssSource(), or.getJavaRule(), sortNo);
    }

    private Long loadScriptTableId(long scriptId) {
        try {
            return jdbc.queryForObject("SELECT table_id FROM sps_script WHERE id=?", Long.class, scriptId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) : s;
    }
}
