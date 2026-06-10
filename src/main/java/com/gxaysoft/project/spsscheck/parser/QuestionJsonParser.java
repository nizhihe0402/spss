package com.gxaysoft.project.spsscheck.parser;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestionJsonParser {
    private QuestionJsonParser() {
    }

    public static Map<String, QuestionMapping> parseQuestionMappings(Path jsonPath, long tableId) throws IOException {
        String text = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
        Map<String, QuestionMapping> mappings = new LinkedHashMap<>();

        // Match each object in the JSON array: { ... }
        Pattern objectPattern = Pattern.compile("\\{[^\\{]+?\\}", Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(text);

        while (objectMatcher.find()) {
            String obj = objectMatcher.group();

            long qId = extractLong(obj, "question_id");
            String ec = extractString(obj, "export_content");
            String content = extractString(obj, "content");
            long tid = extractLong(obj, "table_id");

            if (tid != tableId || qId < 0) {
                continue;
            }

            String variable = firstVariableLike(extractString(obj, "export_sort"), ec);
            if (variable == null) {
                continue;
            }

            String normalized = SpssUtil.normalize(variable);
            if (!mappings.containsKey(normalized)) {
                mappings.put(normalized, new QuestionMapping(qId, variable, content, tid));
            }
        }

        return mappings;
    }

    private static long extractLong(String obj, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(obj);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return -1L;
            }
        }
        // Try null
        Pattern nullP = Pattern.compile("\"" + key + "\"\\s*:\\s*null");
        return nullP.matcher(obj).find() ? -1L : -1L;
    }

    private static String extractString(String obj, String key) {
        // Try string value: "key" : "value"
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(obj);
        if (m.find()) {
            return m.group(1).replace("\\/", "/").replace("\\\"", "\"");
        }
        // Try null
        Pattern nullP = Pattern.compile("\"" + key + "\"\\s*:\\s*null");
        if (nullP.matcher(obj).find()) {
            return null;
        }
        // Try numeric value treated as string
        Pattern numP = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        m = numP.matcher(obj);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String firstVariableLike(String exportSort, String exportContent) {
        if (exportSort != null && looksLikeSpssVariable(exportSort)) {
            return exportSort;
        }
        if (exportContent != null && looksLikeSpssVariable(exportContent)) {
            return exportContent;
        }
        return null;
    }

    private static boolean looksLikeSpssVariable(String value) {
        if (value == null) {
            return false;
        }
        return value.trim().matches("[A-Za-z][A-Za-z0-9_]*");
    }
}
