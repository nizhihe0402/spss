package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.persistence.ScriptService;
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
        return jdbc.queryForList(
            "SELECT id, script_name AS name, parse_status AS parse, status, " +
            "version_no AS ver, (SELECT COUNT(*) FROM sps_rule WHERE script_id=s.id) AS rules, " +
            "table_id AS tableId, project_id AS projectId, project_type AS projectType, year, " +
            "DATE_FORMAT(created_time, '%Y-%m-%d %H:%i') AS time " +
            "FROM sps_script s ORDER BY id DESC");
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
}
