package com.gxaysoft.project.spsscheck.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ScriptController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/scripts")
    public List<Map<String, Object>> list() {
        return jdbc.queryForList(
            "SELECT s.id, s.script_name AS name, s.parse_status AS parse, s.status, s.version_no AS ver, " +
            "(SELECT COUNT(*) FROM sps_rule WHERE script_id=s.id) AS rules, " +
            "DATE_FORMAT(s.created_time,'%Y-%m-%d %H:%i:%s') AS time " +
            "FROM sps_script s ORDER BY s.id");
    }

    @GetMapping("/scripts/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return jdbc.queryForMap("SELECT * FROM sps_script WHERE id=" + id);
    }

    @PutMapping("/scripts/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestParam String status) {
        jdbc.update("UPDATE sps_script SET status=? WHERE id=?", status, id);
        return Collections.singletonMap("code", 0);
    }

    @DeleteMapping("/scripts/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        jdbc.update("DELETE FROM sps_unsupported_statement WHERE script_id=?", id);
        jdbc.update("DELETE FROM sps_rule_step WHERE rule_id IN (SELECT id FROM sps_rule WHERE script_id=?)", id);
        jdbc.update("DELETE FROM sps_output_rule WHERE script_id=?", id);
        jdbc.update("DELETE FROM sps_rule WHERE script_id=?", id);
        jdbc.update("DELETE FROM sps_script WHERE id=?", id);
        return Collections.singletonMap("code", 0);
    }
}
