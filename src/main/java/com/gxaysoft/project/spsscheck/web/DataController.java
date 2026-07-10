package com.gxaysoft.project.spsscheck.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 参考数据查询：项目列表、调查表列表。
 * 供上传页面动态加载下拉选项。
 */
@RestController
@RequestMapping("/api/data")
public class DataController {
    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 项目列表 — 从 bus_project 加载。
     * 返回: [{projectId, projectName, year, interveneFlag}]
     */
    @GetMapping("/projects")
    public List<Map<String, Object>> listProjects() {
        try {
            // 尝试查 intervene_flag 列（部分环境可能后加的列）
            return jdbc.queryForList(
                "SELECT project_id AS projectId, project_name AS projectName, year, " +
                "COALESCE(intervene_flag, '0') AS interveneFlag " +
                "FROM bus_project WHERE del_flag='0' AND status='1' ORDER BY project_id DESC");
        } catch (Exception e) {
            log.warn("查询bus_project失败(可能缺少intervene_flag列): {}", e.getMessage());
            return jdbc.queryForList(
                "SELECT project_id AS projectId, project_name AS projectName, year, " +
                "'0' AS interveneFlag " +
                "FROM bus_project WHERE del_flag='0' AND status='1' ORDER BY project_id DESC");
        }
    }

    /**
     * 单个项目详情 — 取年份和干预标志。
     */
    @GetMapping("/projects/{projectId}")
    public Map<String, Object> getProject(@PathVariable Long projectId) {
        try {
            return jdbc.queryForMap(
                "SELECT project_id AS projectId, project_name AS projectName, year, " +
                "COALESCE(intervene_flag, '0') AS interveneFlag " +
                "FROM bus_project WHERE project_id=? AND del_flag='0'", projectId);
        } catch (Exception e) {
            log.warn("查询项目详情失败: projectId={}, {}", projectId, e.getMessage());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("projectId", projectId);
            fallback.put("year", "");
            fallback.put("interveneFlag", "0");
            return fallback;
        }
    }

    /**
     * 年级列表 — 从 bus_student 加载某学校下不重复的年级。
     */
    @GetMapping("/grades")
    public List<Map<String, Object>> listGrades(@RequestParam Long schoolId) {
        return jdbc.queryForList(
            "SELECT DISTINCT grade AS gradeCode, grade AS gradeName " +
            "FROM bus_student WHERE school_id=? AND del_flag='0' ORDER BY grade", schoolId);
    }

    /**
     * 班级列表 — 从 bus_student 加载某学校某年级下不重复的班级。
     */
    @GetMapping("/classes")
    public List<Map<String, Object>> listClasses(@RequestParam Long schoolId,
                                                  @RequestParam String grade) {
        return jdbc.queryForList(
            "SELECT DISTINCT student_class AS classCode, student_class AS className " +
            "FROM bus_student WHERE school_id=? AND grade=? AND del_flag='0' ORDER BY student_class",
            schoolId, grade);
    }

    /**
     * 调查表列表 — 从 bus_table 加载，可选按 projectId 过滤（通过 bus_project_table 关联）。
     * 返回: [{tableId, tableName}]
     */
    @GetMapping("/tables")
    public List<Map<String, Object>> listTables(
            @RequestParam(value = "projectId", required = false) Long projectId) {
        if (projectId != null && projectId > 0) {
            return jdbc.queryForList(
                "SELECT t.table_id AS tableId, t.table_name AS tableName " +
                "FROM bus_table t " +
                "INNER JOIN bus_project_table pt ON pt.table_id = t.table_id AND pt.del_flag='0' " +
                "WHERE t.del_flag='0' AND pt.project_id=? " +
                "ORDER BY t.table_id", projectId);
        }
        return jdbc.queryForList(
            "SELECT table_id AS tableId, table_name AS tableName " +
            "FROM bus_table WHERE del_flag='0' ORDER BY table_id");
    }
}
