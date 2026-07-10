package com.gxaysoft.project.spsscheck.parser;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestionSqlParser {
    private QuestionSqlParser() {
    }

    public static Map<String, QuestionMapping> parseQuestionMappings(String sqlText, long tableId) {
        Map<String, QuestionMapping> mappings = new LinkedHashMap<>();
        int searchFrom = 0;
        while (true) {
            int insertAt = indexOfIgnoreCase(sqlText, "INSERT INTO", searchFrom);
            if (insertAt < 0) {
                break;
            }
            int columnsStart = sqlText.indexOf('(', insertAt);
            if (columnsStart < 0) {
                break;
            }
            String insertHeader = sqlText.substring(insertAt, columnsStart);
            if (indexOfIgnoreCase(insertHeader, "bus_question", 0) < 0) {
                searchFrom = insertAt + 11;
                continue;
            }
            int valuesAt = indexOfIgnoreCase(sqlText, " VALUES ", columnsStart);
            if (valuesAt < 0) {
                break;
            }
            int statementEnd = sqlText.indexOf(';', valuesAt);
            if (statementEnd < 0) {
                statementEnd = sqlText.length();
            }
            String statement = sqlText.substring(insertAt, statementEnd);
            parseQuestionInsert(statement, tableId, mappings);
            searchFrom = statementEnd + 1;
        }
        return mappings;
    }

    private static void parseQuestionInsert(String statement, long expectedTableId, Map<String, QuestionMapping> mappings) {
        int columnsStart = statement.indexOf('(');
        int columnsEnd = findMatchingParen(statement, columnsStart);
        if (columnsStart < 0 || columnsEnd < 0) {
            return;
        }
        List<String> columns = parseCsvLike(statement.substring(columnsStart + 1, columnsEnd).replace("`", ""));
        int valuesAt = indexOfIgnoreCase(statement, " VALUES ", columnsEnd);
        if (valuesAt < 0) {
            return;
        }
        String values = statement.substring(valuesAt + 8).trim();
        for (String tuple : splitTuples(values)) {
            List<String> row = parseCsvLike(tuple);
            if (row.size() < columns.size()) {
                continue;
            }
            Map<String, String> byColumn = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                byColumn.put(columns.get(i).trim(), cleanSqlValue(row.get(i)));
            }
            long tableId = parseLong(byColumn.get("table_id"), -1L);
            if (tableId != expectedTableId) {
                continue;
            }
            long questionId = parseLong(byColumn.get("question_id"), -1L);
            String variable = QuestionVariableNameSelector.variableNameFromExportContent(byColumn.get("export_content"));
            if (questionId < 0 || variable == null) {
                continue;
            }
            String normalized = SpssUtil.normalize(variable);
            if (!mappings.containsKey(normalized)) {
                mappings.put(normalized, new QuestionMapping(questionId, variable, byColumn.get("content"), tableId));
            }
        }
    }

    private static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), fromIndex);
    }

    private static int findMatchingParen(String text, int start) {
        if (start < 0) {
            return -1;
        }
        int depth = 0;
        boolean inQuote = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && (i + 1 >= text.length() || text.charAt(i + 1) != '\'')) {
                inQuote = !inQuote;
            }
            if (inQuote) {
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<String> splitTuples(String values) {
        List<String> tuples = new ArrayList<>();
        int tupleStart = -1;
        int depth = 0;
        boolean inQuote = false;
        for (int i = 0; i < values.length(); i++) {
            char c = values.charAt(i);
            if (c == '\'') {
                if (inQuote && i + 1 < values.length() && values.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            }
            if (inQuote) {
                continue;
            }
            if (c == '(') {
                if (depth == 0) {
                    tupleStart = i + 1;
                }
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0 && tupleStart >= 0) {
                    tuples.add(values.substring(tupleStart, i));
                }
            }
        }
        return tuples;
    }

    static List<String> parseCsvLike(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                if (inSingleQuote && i + 1 < line.length() && line.charAt(i + 1) == '\'') {
                    current.append('\'');
                    i++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
            } else if (c == '"' && !inSingleQuote) {
                if (inDoubleQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inDoubleQuote = !inDoubleQuote;
                }
            } else if (c == ',' && !inSingleQuote && !inDoubleQuote) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private static String cleanSqlValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ("NULL".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static long parseLong(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
