package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.parser.QuestionVariableNameSelector;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import com.gxaysoft.project.spsscheck.validation.SourceQuestionMappingFormatter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SourceQuestionMappingSyncService {
    private final JdbcTemplate jdbc;

    public SourceQuestionMappingSyncService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String buildForSources(String sources, Long tableId) {
        return SourceQuestionMappingFormatter.formatLookup(sources, loadQuestionLookup(tableId));
    }

    public int syncScript(long scriptId) {
        Long tableId = findScriptTableId(scriptId);
        Map<String, List<QuestionMapping>> lookup = loadQuestionLookup(tableId);
        List<Map<String, Object>> rules = jdbc.queryForList(
                "SELECT id, source_variables FROM sps_rule WHERE script_id=? ORDER BY sort_no", scriptId);
        int updated = 0;
        for (Map<String, Object> rule : rules) {
            Long ruleId = ((Number) rule.get("id")).longValue();
            String sources = stringValue(rule.get("source_variables"));
            String mappings = SourceQuestionMappingFormatter.formatLookup(sources, lookup);
            jdbc.update("UPDATE sps_rule SET source_question_mappings=? WHERE id=?", mappings, ruleId);
            updated++;
        }
        return updated;
    }

    private Long findScriptTableId(long scriptId) {
        try {
            return jdbc.queryForObject("SELECT table_id FROM sps_script WHERE id=?", Long.class, scriptId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, List<QuestionMapping>> loadQuestionLookup(Long tableId) {
        Map<String, List<QuestionMapping>> filtered = queryQuestionLookup(tableId, true);
        if (!filtered.isEmpty() || tableId == null || tableId.longValue() <= 0) {
            return filtered;
        }
        return queryQuestionLookup(null, true);
    }

    private Map<String, List<QuestionMapping>> queryQuestionLookup(Long tableId, boolean withDelFlag) {
        String sql = "SELECT question_id, table_id, content, export_content FROM bus_question";
        List<Object> args = new ArrayList<Object>();
        List<String> where = new ArrayList<String>();
        if (tableId != null && tableId.longValue() > 0) {
            where.add("table_id=?");
            args.add(tableId);
        }
        if (withDelFlag) {
            where.add("(del_flag IS NULL OR del_flag <> '2')");
        }
        if (!where.isEmpty()) {
            sql += " WHERE " + join(where, " AND ");
        }
        sql += " ORDER BY table_id, question_id";
        try {
            return buildLookup(jdbc.queryForList(sql, args.toArray()));
        } catch (DataAccessException ex) {
            if (withDelFlag) {
                return queryQuestionLookup(tableId, false);
            }
            return new LinkedHashMap<String, List<QuestionMapping>>();
        }
    }

    private Map<String, List<QuestionMapping>> buildLookup(List<Map<String, Object>> rows) {
        Map<String, List<QuestionMapping>> lookup = new LinkedHashMap<String, List<QuestionMapping>>();
        for (Map<String, Object> row : rows) {
            long questionId = asLong(row.get("question_id"), -1L);
            long tableId = asLong(row.get("table_id"), -1L);
            String content = stringValue(row.get("content"));
            String variable = QuestionVariableNameSelector.variableNameFromExportContent(
                    stringValue(row.get("export_content")));
            putMapping(lookup, variable, questionId, content, tableId);
        }
        return lookup;
    }

    private void putMapping(Map<String, List<QuestionMapping>> lookup, Object variableValue,
                            long questionId, String content, long tableId) {
        String variable = stringValue(variableValue);
        if (variable.isEmpty() || questionId <= 0) return;
        String key = SpssUtil.normalize(variable);
        List<QuestionMapping> mappings = lookup.get(key);
        if (mappings == null) {
            mappings = new ArrayList<QuestionMapping>();
            lookup.put(key, mappings);
        }
        mappings.add(new QuestionMapping(questionId, variable, content, tableId));
    }

    private static long asLong(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(values.get(i));
        }
        return sb.toString();
    }
}
