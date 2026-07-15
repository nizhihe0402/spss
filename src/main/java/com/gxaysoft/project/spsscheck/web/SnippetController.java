package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
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

            ParsedScript parsed = SpssParser.parse(spsText);
            List<Rule> rules = parsed.getRules();

            Integer maxSort = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_no),0) FROM sps_rule WHERE script_id=?", Integer.class, scriptId);
            int sortNo = maxSort != null ? maxSort : 0;

            for (Rule rd : rules) {
                sortNo++;
                insertRuleV2(scriptId, sortNo, rd);
            }

            result.put("code", 0);
            result.put("rules", rules.size());
            result.put("msg", "成功追加 " + rules.size() + " 条规则");
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

            ParsedScript parsed = SpssParser.parse(spsText);
            List<Rule> rules = parsed.getRules();

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
            for (Rule rd : rules) {
                sortNo++;
                insertRuleV2(scriptId, sortNo, rd);
            }

            result.put("code", 0);
            result.put("scriptId", scriptId);
            result.put("tableId", scriptTableId);
            result.put("rules", rules.size());
            result.put("msg", "新建脚本 #" + scriptId + ", " + rules.size() + " 条规则");
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
            ParsedScript parsed = SpssParser.parse(spsText);
            List<Rule> rules = parsed.getRules();

            List<Map<String, Object>> ruleList = new ArrayList<>();
            for (Rule r : rules) {
                String sources = String.join(",", r.getSourceVariables());
                RuleType rt = r.getType() != null ? r.getType() : RuleType.CONDITIONAL_BLOCK;
                RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                        rt.name(), r.getTarget(), sources, r.getDescription());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("target", r.getTarget());
                m.put("type", rt.name());
                m.put("typeLabel", rt.label);
                m.put("description", r.getDescription());
                m.put("sources", sources);
                m.put("expression", r.getExpression());
                m.put("javaPreview", r.getJavaPreview());
                m.put("executionChain", r.getExecutionChain());
                m.put("spssSource", truncate(r.getSpssSource() != null ? r.getSpssSource() : "", 500));
                m.put("correctionEnabled", correction.enabled ? 1 : 0);
                m.put("correctionDescription", correction.description);

                // 步骤列表
                List<Map<String, Object>> stepList = new ArrayList<>();
                if (r.getSteps() != null) {
                    int sno = 0;
                    for (com.gxaysoft.project.spsscheck.engine.model.Step s : r.getSteps()) {
                        sno++;
                        Map<String, Object> sm = new LinkedHashMap<>();
                        sm.put("no", sno);
                        sm.put("condition", s.getCondition());
                        sm.put("target", s.getTarget());
                        if (s.getAction() instanceof com.gxaysoft.project.spsscheck.engine.model.ComputeAction) {
                            sm.put("type", "COMPUTE");
                            sm.put("expression", ((com.gxaysoft.project.spsscheck.engine.model.ComputeAction) s.getAction()).getExpression());
                        } else if (s.getAction() instanceof com.gxaysoft.project.spsscheck.engine.model.RecodeAction) {
                            sm.put("type", "RECODE");
                            sm.put("source", ((com.gxaysoft.project.spsscheck.engine.model.RecodeAction) s.getAction()).getSource());
                        } else if (s.getAction() instanceof com.gxaysoft.project.spsscheck.engine.model.IfAssignAction) {
                            sm.put("type", "IF_ASSIGN");
                            sm.put("value", ((com.gxaysoft.project.spsscheck.engine.model.IfAssignAction) s.getAction()).getValue());
                        }
                        stepList.add(sm);
                    }
                }
                m.put("steps", stepList);
                ruleList.add(m);
            }

            result.put("code", 0);
            result.put("rules", ruleList);
            result.put("totalRules", rules.size());
        } catch (Exception e) {
            result.put("code", 1); result.put("msg", "解析失败: " + e.getMessage());
        }
        return result;
    }

    private void insertRuleV2(long scriptId, int sortNo, Rule rd) {
        String code = String.format("R%03d", sortNo);
        String sources = String.join(",", rd.getSourceVariables());
        String sourceQuestionMappings = new SourceQuestionMappingSyncService(jdbc).buildForSources(sources, loadScriptTableId(scriptId));
        RuleType rt = rd.getType() != null ? rd.getType() : RuleType.CONDITIONAL_BLOCK;
        RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                rt.name(), rd.getTarget(), sources, rd.getDescription());
        String spssSource = rd.getSpssSource() != null ? rd.getSpssSource() : "";
        String javaPreview = rd.getJavaPreview() != null ? rd.getJavaPreview() : "";
        jdbc.update(
            "INSERT INTO sps_rule (script_id, rule_code, rule_name, rule_type, target_variable, " +
            "source_variables, source_question_mappings, correction_enabled, correction_type, correction_variables, " +
            "correction_source, correction_strategy, correction_apply_stage, correction_write_clean, " +
            "correction_write_source, correction_description, spss_source, rule_json, java_preview, sort_no, affect_clean, warning_message) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            scriptId, code, rd.getTarget(), rt.name(),
            rd.getTarget(), sources, sourceQuestionMappings,
            correction.enabled ? 1 : 0, correction.type, correction.variables,
            correction.source, correction.strategy, correction.applyStage,
            correction.writeClean ? 1 : 0, correction.writeSource ? 1 : 0,
            correction.description,
            truncate(spssSource, 65535),
            "{\"v2\":true,\"type\":\"" + rt.name() + "\"}",
            javaPreview, sortNo,
            rt == RuleType.IDENTITY_CHECK
                    || rt == RuleType.MISSING_CHECK
                    || rt == RuleType.RANGE_CHECK
                    || rt == RuleType.CONSISTENCY_CHECK
                    || rt == RuleType.DOCUMENT_CHECK ? 1 : 0,
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

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) : s;
    }
}
