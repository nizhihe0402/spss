package com.gxaysoft.project.spsscheck.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.gxaysoft.project.spsscheck.persistence.SourceQuestionMappingSyncService;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RuleController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/rules")
    public List<Map<String, Object>> list(@RequestParam Long scriptId) {
        return jdbc.queryForList(
            "SELECT id, rule_code AS code, rule_name AS name, rule_type AS type, " +
            "target_variable AS target, source_variables AS sources, " +
            "source_question_mappings AS sourceQuestionMappings, " +
            "warning_message AS description, sort_no AS sort, " +
            "start_line AS startLine, end_line AS endLine, " +
            "correction_enabled AS correctionEnabled, correction_type AS correctionType, " +
            "correction_variables AS correctionVariables, correction_strategy AS correctionStrategy, " +
            "correction_source AS correctionSource, correction_apply_stage AS correctionApplyStage " +
            "FROM sps_rule WHERE script_id=? ORDER BY sort_no", scriptId);
    }

    @GetMapping("/rules/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Map<String, Object> rule = jdbc.queryForMap(
            "SELECT id, rule_code AS code, rule_name AS name, rule_type AS type, " +
            "target_variable AS target, source_variables AS sources, enabled, " +
            "source_question_mappings AS sourceQuestionMappings, " +
            "spss_source AS spss, java_preview AS java, rule_json AS ruleJson, " +
            "warning_message AS description, " +
            "correction_enabled AS correctionEnabled, correction_type AS correctionType, " +
            "correction_variables AS correctionVariables, correction_source AS correctionSource, " +
            "correction_apply_stage AS correctionApplyStage, correction_write_clean AS correctionWriteClean, " +
            "correction_write_source AS correctionWriteSource, correction_strategy AS correctionStrategy, " +
            "correction_description AS correctionDescription " +
            "FROM sps_rule WHERE id=?", id);
        List<Map<String, Object>> steps = jdbc.queryForList(
            "SELECT step_no AS no, step_type AS type, condition_text AS `condition`, " +
            "source_variable AS source, target_variable AS target, expression_text AS expression, " +
            "recode_json AS recode FROM sps_rule_step WHERE rule_id=? ORDER BY step_no", id);
        rule.put("steps", steps);
        return rule;
    }

    @PutMapping("/rules/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String target = body.get("target");
        String sources = body.get("sources");
        String desc = body.get("description");
        String sourceQuestionMappings = new SourceQuestionMappingSyncService(jdbc).buildForSources(sources, null);
        jdbc.update("UPDATE sps_rule SET rule_name=?, target_variable=?, source_variables=?, source_question_mappings=?, warning_message=? WHERE id=?",
                name, target, sources, sourceQuestionMappings, desc, id);
        return Collections.singletonMap("code", 0);
    }

    @PutMapping("/rules/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id, @RequestParam int enabled) {
        jdbc.update("UPDATE sps_rule SET enabled=? WHERE id=?", enabled, id);
        return Collections.singletonMap("code", 0);
    }

    @PutMapping("/rules/{id}/correction")
    public Map<String, Object> updateCorrection(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbc.update("UPDATE sps_rule SET correction_enabled=?, correction_type=?, " +
            "correction_variables=?, correction_source=?, correction_apply_stage=?, " +
            "correction_write_clean=?, correction_write_source=?, correction_strategy=?, " +
            "correction_description=? WHERE id=?",
            body.get("correctionEnabled"), body.get("correctionType"),
            body.get("correctionVariables"), body.get("correctionSource"),
            body.get("correctionApplyStage"), body.get("correctionWriteClean"),
            body.get("correctionWriteSource"), body.get("correctionStrategy"),
            body.get("correctionDescription"), id);
        return Collections.singletonMap("code", 0);
    }

    @PostMapping("/rules/sync-source-question-mappings")
    public Map<String, Object> syncSourceQuestionMappings(@RequestParam Long scriptId) {
        int updated = new SourceQuestionMappingSyncService(jdbc).syncScript(scriptId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("updated", updated);
        return result;
    }
}
