package com.gxaysoft.project.spsscheck.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
public class RegionSchoolController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/regions/children")
    public Map<String, Object> regionChildren(@RequestParam(value = "parentId", required = false) Long parentId,
                                              @RequestParam(value = "parent_id", required = false) Long parentId2) {
        long actualParentId = firstPositive(parentId, parentId2);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT division_id AS id, division_name AS name, parent_id AS parentId " +
                        "FROM sys_divisionmsg WHERE del_flag=0 AND " +
                        (actualParentId > 0 ? "parent_id=? " : "(parent_id IS NULL OR parent_id=0) ") +
                        "ORDER BY sort, division_id",
                actualParentId > 0 ? new Object[]{actualParentId} : new Object[]{});
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("code", 0);
        result.put("data", rows);
        return result;
    }

    @GetMapping("/schools")
    public Map<String, Object> schools(@RequestParam(value = "divisionId", required = false) Long divisionId,
                                       @RequestParam(value = "division_id", required = false) Long divisionId2,
                                       @RequestParam(value = "projectId", required = false) Long projectId,
                                       @RequestParam(value = "project_id", required = false) Long projectId2) {
        long actualDivisionId = firstPositive(divisionId, divisionId2);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (actualDivisionId <= 0) {
            result.put("code", 400);
            result.put("msg", "divisionId 必填");
            return result;
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT school_id AS id, school_name AS name, school_code AS schoolCode, " +
                        "school_type AS schoolType, division_id AS divisionId, is_intervene AS isIntervene " +
                        "FROM bus_school WHERE division_id=? AND status='0' AND del_flag='0' " +
                        "ORDER BY order_num, school_id",
                actualDivisionId);
        result.put("code", 0);
        result.put("projectId", firstPositive(projectId, projectId2));
        result.put("divisionId", actualDivisionId);
        result.put("data", rows);
        return result;
    }

    private long firstPositive(Long first, Long second) {
        if (first != null && first.longValue() > 0) return first.longValue();
        if (second != null && second.longValue() > 0) return second.longValue();
        return -1L;
    }
}
