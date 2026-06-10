package com.gxaysoft.project.spsscheck.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UnsupportedController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/unsupported")
    public List<Map<String, Object>> list(@RequestParam Long scriptId) {
        return jdbc.queryForList(
            "SELECT id, statement_type AS type, reason, risk_level AS risk " +
            "FROM sps_unsupported_statement WHERE script_id=? ORDER BY id", scriptId);
    }
}
