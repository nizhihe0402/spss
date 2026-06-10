package com.gxaysoft.project.spsscheck.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.gxaysoft.project.spsscheck.v2.model.RuleType;

import java.util.*;

/** V2 API: serves rules parsed by BlockParser with RuleType classification */
@RestController
@RequestMapping("/api/v2")
public class RuleControllerV2 {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/rules")
    public List<Map<String, Object>> list(@RequestParam Long scriptId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT id, rule_code AS code, rule_name AS name, rule_type AS type, " +
            "target_variable AS target, source_variables AS sources, " +
            "warning_message AS description, sort_no AS sort, " +
            "start_line AS startLine, end_line AS endLine, line_count AS lineCount, " +
            "segment_title AS segmentTitle, split_reason AS splitReason " +
            "FROM sps_rule WHERE script_id=? ORDER BY sort_no", scriptId);
        addTypeLabels(rows);
        return rows;
    }

    @GetMapping("/rules/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Map<String, Object> rule = jdbc.queryForMap(
            "SELECT id, rule_code AS code, rule_name AS name, rule_type AS type, " +
            "target_variable AS target, source_variables AS sources, " +
            "spss_source AS spss, java_preview AS java, warning_message AS description, " +
            "start_line AS startLine, end_line AS endLine, line_count AS lineCount, " +
            "segment_title AS segmentTitle, split_reason AS splitReason " +
            "FROM sps_rule WHERE id=?", id);
        addTypeLabel(rule);
        return rule;
    }

    @GetMapping("/summary")
    public List<Map<String, Object>> summary(@RequestParam Long scriptId) {
        return jdbc.queryForList(
            "SELECT rule_type AS type, COUNT(*) AS count " +
            "FROM sps_rule WHERE script_id=? GROUP BY rule_type ORDER BY count DESC", scriptId);
    }


    private void addTypeLabels(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            addTypeLabel(row);
        }
    }

    private void addTypeLabel(Map<String, Object> row) {
        Object type = row.get("type");
        row.put("typeLabel", labelOf(type == null ? null : String.valueOf(type)));
    }

    private String labelOf(String type) {
        if (type == null || type.trim().isEmpty()) return "";
        try {
            return RuleType.valueOf(type).label;
        } catch (IllegalArgumentException ex) {
            return type;
        }
    }

    @PutMapping("/rules/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        jdbc.update("UPDATE sps_rule SET rule_name=?, target_variable=?, source_variables=?, warning_message=? WHERE id=?",
                body.get("name"), body.get("target"), body.get("sources"), body.get("description"), id);
        return Collections.singletonMap("code", 0);
    }

    /** Re-generate java_preview for all rules in a script */
    @PostMapping("/refresh-java/{scriptId}")
    public Map<String, Object> refreshJava(@PathVariable Long scriptId) {
        List<Map<String, Object>> rules = jdbc.queryForList(
            "SELECT * FROM sps_rule WHERE script_id=? ORDER BY sort_no", scriptId);
        int updated = 0;
        for (Map<String, Object> row : rules) {
            String typeStr = String.valueOf(row.get("rule_type"));
            String target = String.valueOf(row.getOrDefault("target_variable", ""));
            String spss = String.valueOf(row.getOrDefault("spss_source", ""));
            String sources = String.valueOf(row.getOrDefault("source_variables", ""));
            String desc = String.valueOf(row.getOrDefault("warning_message", ""));
            Long ruleId = ((Number) row.get("id")).longValue();

            String javaPreview = buildJavaPreview(typeStr, target, spss, sources, desc);
            jdbc.update("UPDATE sps_rule SET java_preview=? WHERE id=?", javaPreview, ruleId);
            updated++;
        }
        return Collections.singletonMap("code", 0);
    }

    private String buildJavaPreview(String typeStr, String target, String spss, String sources, String desc) {
        StringBuilder sb = new StringBuilder();
        boolean isCheck = false;
        switch (typeStr) {
            case "COMPUTE_INTERMEDIATE":
                sb.append("// ").append(desc != null && !desc.isEmpty() ? desc : "计算 " + target).append("\n");
                String expr = extractExpression(spss);
                sb.append("BigDecimal ").append(safeIdent(target))
                  .append(" = new ArithmeticExpression(\"")
                  .append(expr.replace("\"", "\\\""))
                  .append("\", row).parse();");
                if (sources != null && !sources.isEmpty())
                    sb.append(" // 依赖: ").append(sources);
                break;
            case "IDENTITY_CHECK":
            case "MISSING_CHECK":
            case "CONSISTENCY_CHECK":
            case "RANGE_CHECK":
            case "DOCUMENT_CHECK":
            case "OUTCOME_DETERMINATION":
                sb.append("// ").append(desc != null && !desc.isEmpty() ? desc : "检查 " + target).append("\n");
                sb.append("int flag_").append(safeIdent(target)).append(" = check_").append(safeIdent(target))
                  .append("(row); // 返回0=正常 1=异常\n");
                sb.append("row.put(\"").append(target).append("\", flag_").append(safeIdent(target)).append(");\n");
                sb.append("row.putFlag(\"").append(target).append("\", flag_").append(safeIdent(target)).append(");\n");
                sb.append("// 原始SPSS:\n// ").append(spss.replace("\n", "\n// "));
                break;
            case "DUPLICATE_MARK":
                sb.append("// 去重标记\n");
                sb.append("rows.sort(Comparator.comparing(r -> r.getDecimal(\"").append(target).append("\")));\n");
                sb.append("// 按").append(target).append("分组，每组首条PrimaryFirst1=1，末条PrimaryLast=1");
                break;
            case "OUTPUT_GROUP":
                sb.append("// 输出分组: ").append(target).append("\n");
                sb.append("// SPSS: ").append(spss.replaceAll("\\s+", " ").substring(0, Math.min(100, spss.length())));
                break;
            default:
                sb.append("// ").append(typeStr).append(": ").append(target).append("\n");
                sb.append("// SPSS: ").append(spss.replaceAll("\\s+", " ").substring(0, Math.min(100, spss.length())));
                break;
        }
        return sb.toString();
    }

    private String extractExpression(String spss) {
        if (spss == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?i)COMPUTE\\s+[^=]+=\\s*(.+?)\\.", java.util.regex.Pattern.DOTALL).matcher(spss);
        return m.find() ? m.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    private String safeIdent(String s) {
        return s == null ? "var" : s.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
    }
}
