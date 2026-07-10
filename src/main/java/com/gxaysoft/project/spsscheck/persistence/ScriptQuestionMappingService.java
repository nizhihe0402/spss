package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.parser.QuestionVariableNameSelector;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScriptQuestionMappingService {
    private final JdbcTemplate jdbc;

    public ScriptQuestionMappingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, QuestionMapping> loadVariableMappings(long scriptId) {
        List<QuestionMapping> mappings = loadScriptMappings(scriptId);
        if (mappings.isEmpty()) {
            Long tableId = findScriptTableId(scriptId);
            if (tableId != null && tableId.longValue() > 0) {
                mappings = loadQuestionMappingsByTable(tableId.longValue());
            }
        }
        return toVariableMappings(mappings);
    }

    public List<QuestionMapping> loadScriptMappings(long scriptId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT variable_name, question_id, question_content, source_table_id " +
                            "FROM sps_script_question_mapping WHERE script_id=? ORDER BY sort_no, id", scriptId);
            List<QuestionMapping> mappings = new ArrayList<QuestionMapping>();
            for (Map<String, Object> row : rows) {
                long questionId = asLong(row.get("question_id"), -1L);
                String variable = stringValue(row.get("variable_name"));
                long tableId = asLong(row.get("source_table_id"), -1L);
                if (questionId > 0 && !variable.isEmpty()) {
                    mappings.add(new QuestionMapping(questionId, variable, stringValue(row.get("question_content")), tableId));
                }
            }
            return mappings;
        } catch (Exception ex) {
            return new ArrayList<QuestionMapping>();
        }
    }

    private Long findScriptTableId(long scriptId) {
        try {
            return jdbc.queryForObject("SELECT table_id FROM sps_script WHERE id=?", Long.class, scriptId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<QuestionMapping> loadQuestionMappingsByTable(long tableId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT question_id, table_id, content, export_content FROM bus_question " +
                        "WHERE table_id=? AND (del_flag IS NULL OR del_flag='0') ORDER BY question_id", tableId);
        return toQuestionMappings(rows);
    }

    public static List<QuestionMapping> loadQuestionMappings(Connection conn, long tableId) throws SQLException {
        List<QuestionMapping> mappings = new ArrayList<QuestionMapping>();
        String sql = "SELECT question_id, table_id, content, export_content FROM bus_question " +
                "WHERE table_id=? AND (del_flag IS NULL OR del_flag='0') ORDER BY question_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String variable = QuestionVariableNameSelector.variableNameFromExportContent(rs.getString("export_content"));
                    if (variable == null || variable.trim().isEmpty()) continue;
                    mappings.add(new QuestionMapping(rs.getLong("question_id"), variable,
                            rs.getString("content"), rs.getLong("table_id")));
                }
            }
        }
        return mappings;
    }

    public static Map<String, QuestionMapping> toVariableMappings(List<QuestionMapping> mappings) {
        Map<String, QuestionMapping> byVariable = new LinkedHashMap<String, QuestionMapping>();
        if (mappings == null) {
            return byVariable;
        }
        for (QuestionMapping mapping : mappings) {
            if (mapping == null || mapping.getQuestionId() <= 0) continue;
            String variable = mapping.getVariableNameOriginal();
            if (variable == null || variable.trim().isEmpty()) continue;
            String key = SpssUtil.normalize(variable);
            if (!byVariable.containsKey(key)) {
                byVariable.put(key, mapping);
            }
        }
        return byVariable;
    }

    public static long inferTableIdFromScriptName(String scriptName) {
        if (scriptName == null) return -1L;
        String name = scriptName.replace(".sps", "").trim();
        if ("表1-1".equals(name)) return 1L;
        if ("表1-2".equals(name)) return 2L;
        if ("表2-1".equals(name)) return 3L;
        if ("表2-2".equals(name)) return 4L;
        if ("表2-3".equals(name)) return 5L;
        if ("表3-1".equals(name)) return 6L;
        if ("表3-2".equals(name)) return 7L;
        if ("表3-3".equals(name)) return 8L;
        if ("表1-3".equals(name)) return 10L;
        return -1L;
    }

    private static List<QuestionMapping> toQuestionMappings(List<Map<String, Object>> rows) {
        List<QuestionMapping> mappings = new ArrayList<QuestionMapping>();
        for (Map<String, Object> row : rows) {
            String variable = QuestionVariableNameSelector.variableNameFromExportContent(stringValue(row.get("export_content")));
            if (variable == null || variable.trim().isEmpty()) continue;
            long questionId = asLong(row.get("question_id"), -1L);
            if (questionId <= 0) continue;
            mappings.add(new QuestionMapping(questionId, variable, stringValue(row.get("content")),
                    asLong(row.get("table_id"), -1L)));
        }
        return mappings;
    }

    private static long asLong(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
