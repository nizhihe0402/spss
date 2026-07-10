package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.parser.QuestionVariableNameSelector;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ScriptService {
    private static final Logger log = LoggerFactory.getLogger(ScriptService.class);
    private final JdbcTemplate jdbc;

    public ScriptService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public ParsedScript saveAndParse(Long scriptId, String spsText) {
        log.info("保存并解析脚本: scriptId={}, length={}", scriptId, spsText != null ? spsText.length() : 0);
        jdbc.update("UPDATE sps_script SET script_content=?, updated_time=NOW() WHERE id=?",
                spsText, scriptId);
        ParsedScript parsed = SpssParser.parse(spsText);
        log.info("解析完成: rules={}, datasetRules={}, outputRules={}",
                parsed.totalRules(), parsed.totalDatasetRules(), parsed.totalOutputRules());
        return parsed;
    }

    public ParsedScript getParsedRules(Long scriptId) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT script_content FROM sps_script WHERE id=?", scriptId);
        Object value = row.get("script_content");
        String spsText = value == null ? "" : String.valueOf(value);
        return SpssParser.parse(spsText);
    }

    public Map<String, QuestionMapping> loadVariableMappings(Long scriptId) {
        Map<String, QuestionMapping> mappings = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT sqm.variable_name, sqm.question_id, " +
                    "bq.content, bq.export_content, sqm.source_table_id " +
                    "FROM sps_script_question_mapping sqm " +
                    "LEFT JOIN bus_question bq ON bq.question_id=sqm.question_id AND bq.del_flag='0' " +
                    "WHERE sqm.script_id=? ORDER BY sqm.variable_name",
                    scriptId);
            for (Map<String, Object> r : rows) {
                String variable = QuestionVariableNameSelector.variableNameFromExportContent(
                        stringValue(r.get("export_content")));
                if (variable == null || variable.isEmpty()) continue;
                long questionId = asLong(r.get("question_id"), -1L);
                long tableId = asLong(r.get("source_table_id"), 0L);
                if (questionId <= 0) continue;
                mappings.put(SpssUtil.normalize(variable),
                        new QuestionMapping(questionId, variable, stringValue(r.get("content")), tableId));
            }
        } catch (Exception e) {
            log.warn("加载变量映射失败: scriptId={}, error={}", scriptId, e.getMessage());
        }
        return mappings;
    }

    public String loadScriptContent(Long scriptId) {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT script_content FROM sps_script WHERE id=?", scriptId);
            Object value = row.get("script_content");
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            log.warn("加载脚本内容失败: scriptId={}", scriptId, e);
            return "";
        }
    }

    private String stringValue(Object v) { return v == null ? "" : String.valueOf(v); }
    private long asLong(Object v, long def) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v == null) return def;
        try { return Long.parseLong(String.valueOf(v).trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
