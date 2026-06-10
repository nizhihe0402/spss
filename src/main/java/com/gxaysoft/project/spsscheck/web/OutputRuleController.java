package com.gxaysoft.project.spsscheck.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class OutputRuleController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/outputs")
    public List<Map<String, Object>> list(@RequestParam Long scriptId) {
        return jdbc.queryForList(
            "SELECT id, output_code AS code, output_name AS name, output_type AS type, " +
            "select_condition AS `condition`, sort_no AS sort " +
            "FROM sps_output_rule WHERE script_id=? ORDER BY sort_no", scriptId);
    }
}
