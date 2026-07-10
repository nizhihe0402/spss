package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.v2.model.RuleDefinition;
import com.gxaysoft.project.spsscheck.v2.model.RuleType;
import com.gxaysoft.project.spsscheck.v2.parser.BlockParser;
import com.gxaysoft.project.spsscheck.v1.model.SpssOutputRule;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;
import com.gxaysoft.project.spsscheck.persistence.ScriptQuestionMappingService;
import com.gxaysoft.project.spsscheck.persistence.RuleCorrectionPlan;
import com.gxaysoft.project.spsscheck.persistence.SourceQuestionMappingSyncService;
import com.gxaysoft.project.spsscheck.persistence.SpsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@RestController
@RequestMapping("/api")
public class SnippetController {

    @Autowired
    private JdbcTemplate jdbc;

    /** Parse SPSS snippet and append rules to an existing script (V2 engine) */
    @PostMapping("/snippet/append")
    public Map<String, Object> append(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            long scriptId = Long.parseLong(body.getOrDefault("scriptId", "0"));
            String spsText = body.get("spsText");
            if (spsText == null || spsText.trim().isEmpty()) {
                result.put("code", 1); result.put("msg", "SPSS代码不能为空"); return result;
            }

            List<RuleDefinition> rules = BlockParser.parse(spsText);
            List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);

            Integer maxSort = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_no),0) FROM sps_rule WHERE script_id=?", Integer.class, scriptId);
            int sortNo = maxSort != null ? maxSort : 0;

            for (RuleDefinition rd : rules) {
                sortNo++;
                insertRuleV2(scriptId, sortNo, rd);
            }

            Integer maxOut = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_no),0) FROM sps_output_rule WHERE script_id=?", Integer.class, scriptId);
            int outNo = maxOut != null ? maxOut : 0;
            for (SpssOutputRule or : outputRules) {
                outNo++;
                insertOutputRuleInternal(scriptId, outNo, or);
            }

            result.put("code", 0);
            result.put("rules", rules.size());
            result.put("outputRules", outputRules.size());
            result.put("msg", "成功追加 " + rules.size() + " 条规则(V2)");
        } catch (Exception e) {
            result.put("code", 1); result.put("msg", "解析失败: " + e.getMessage());
        }
        return result;
    }

    /** Create a new script from SPSS snippet (V2 engine) */
    @PostMapping("/snippet/create")
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String name = body.getOrDefault("name", "未命名片段");
            String spsText = body.get("spsText");
            long scriptTableId = resolveScriptTableId(body, name);
            if (spsText == null || spsText.trim().isEmpty()) {
                result.put("code", 1); result.put("msg", "SPSS代码不能为空"); return result;
            }

            List<RuleDefinition> rules = BlockParser.parse(spsText);
            List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO sps_script (script_name, script_content, table_code, table_id, parse_status, version_no, status) " +
                    "VALUES (?, ?, ?, ?, 'PARSED', 1, 'DRAFT')", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, spsText);
                ps.setString(3, name);
                ps.setLong(4, scriptTableId);
                return ps;
            }, keyHolder);
            long scriptId = keyHolder.getKey().longValue();
            insertScriptQuestionMappings(scriptId, scriptTableId);

            int sortNo = 0;
            for (RuleDefinition rd : rules) {
                sortNo++;
                insertRuleV2(scriptId, sortNo, rd);
            }
            int outNo = 0;
            for (SpssOutputRule or : outputRules) {
                outNo++;
                insertOutputRuleInternal(scriptId, outNo, or);
            }

            result.put("code", 0);
            result.put("scriptId", scriptId);
            result.put("tableId", scriptTableId);
            result.put("rules", rules.size());
            result.put("outputRules", outputRules.size());
            result.put("msg", "新建脚本 #" + scriptId + ", " + rules.size() + " 条规则(V2)");
        } catch (Exception e) {
            result.put("code", 1); result.put("msg", "解析失败: " + e.getMessage());
        }
        return result;
    }

    /** Preview parse result without saving (V2 engine) */
    @PostMapping("/snippet/preview")
    public Map<String, Object> preview(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String spsText = body.getOrDefault("spsText", "");
            List<RuleDefinition> rules = BlockParser.parse(spsText);
            List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);

            List<Map<String, Object>> ruleList = new ArrayList<>();
            for (RuleDefinition r : rules) {
                String sources = String.join(",", r.getSourceVariables());
                RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                        r.getType().name(), r.getTarget(), sources, r.getDescription());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("target", r.getTarget());
                m.put("type", r.getType().name());
                m.put("typeLabel", r.getType().label);
                m.put("description", r.getDescription());
                m.put("sources", sources);
                m.put("expression", r.getExpression());
                m.put("correctionEnabled", correction.enabled ? 1 : 0);
                m.put("correctionDescription", correction.description);
                ruleList.add(m);
            }

            result.put("code", 0);
            result.put("rules", ruleList);
            result.put("totalRules", rules.size());
            result.put("totalOutputs", outputRules.size());
        } catch (Exception e) {
            result.put("code", 1); result.put("msg", "解析失败: " + e.getMessage());
        }
        return result;
    }

    private void insertRuleV2(long scriptId, int sortNo, RuleDefinition rd) {
        String code = String.format("R%03d", sortNo);
        String sources = String.join(",", rd.getSourceVariables());
        String sourceQuestionMappings = new SourceQuestionMappingSyncService(jdbc).buildForSources(sources, loadScriptTableId(scriptId));
        RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                rd.getType().name(), rd.getTarget(), sources, rd.getDescription());
        jdbc.update(
            "INSERT INTO sps_rule (script_id, rule_code, rule_name, rule_type, target_variable, " +
            "source_variables, source_question_mappings, correction_enabled, correction_type, correction_variables, " +
            "correction_source, correction_strategy, correction_apply_stage, correction_write_clean, " +
            "correction_write_source, correction_description, spss_source, rule_json, java_preview, sort_no, affect_clean, warning_message) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            scriptId, code, rd.getTarget(), rd.getType().name(),
            rd.getTarget(), sources, sourceQuestionMappings,
            correction.enabled ? 1 : 0, correction.type, correction.variables,
            correction.source, correction.strategy, correction.applyStage,
            correction.writeClean ? 1 : 0, correction.writeSource ? 1 : 0,
            correction.description,
            truncate(rd.getSpssBlock(), 65535),
            "{\"v2\":true,\"type\":\"" + rd.getType().name() + "\"}",
            rd.getJavaPreview(), sortNo,
            rd.getType() == RuleType.IDENTITY_CHECK
                    || rd.getType() == RuleType.MISSING_CHECK
                    || rd.getType() == RuleType.RANGE_CHECK
                    || rd.getType() == RuleType.CONSISTENCY_CHECK
                    || rd.getType() == RuleType.DOCUMENT_CHECK ? 1 : 0,
            rd.getDescription());
    }

    private long resolveScriptTableId(Map<String, String> body, String scriptName) {
        long requested = parseLong(body.get("tableId"), -1L);
        if (requested <= 0) requested = parseLong(body.get("table_id"), -1L);
        return requested > 0 ? requested : ScriptQuestionMappingService.inferTableIdFromScriptName(scriptName);
    }

    private long parseLong(String value, long fallback) {
        try {
            return value == null || value.trim().isEmpty() ? fallback : Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Long loadScriptTableId(long scriptId) {
        try {
            return jdbc.queryForObject("SELECT table_id FROM sps_script WHERE id=?", Long.class, scriptId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void insertScriptQuestionMappings(long scriptId, long tableId) throws Exception {
        if (tableId <= 0 || jdbc.getDataSource() == null) {
            return;
        }
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            new SpsRepository(conn).insertScriptQuestionMappings(
                    scriptId, ScriptQuestionMappingService.loadQuestionMappings(conn, tableId));
        }
    }

    private void insertOutputRuleInternal(long scriptId, int sortNo, SpssOutputRule or) {
        String code = String.format("O%03d", sortNo);
        String type = or.getSheetName().contains("清理后") ? "CLEAN_DATA" : "ERROR_GROUP";
        jdbc.update("INSERT INTO sps_output_rule (script_id, output_code, output_name, output_type, " +
                "select_condition, spss_source, java_preview, sort_no) VALUES (?,?,?,?,?,?,?,?)",
                scriptId, code, or.getSheetName(), type,
                or.getCondition(), or.getSpssSource(), or.getJavaRule(), sortNo);
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) : s;
    }
}
