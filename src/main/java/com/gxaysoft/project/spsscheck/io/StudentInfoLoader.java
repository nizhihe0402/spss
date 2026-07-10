package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudentInfoLoader {
    private StudentInfoLoader() {
    }

    public static LoadResult load(Path jsonPath) throws IOException {
        String text = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
        Map<String, Map<String, String>> studentInfo = new LinkedHashMap<String, Map<String, String>>();
        Map<String, QuestionMapping> mappings = new LinkedHashMap<String, QuestionMapping>();

        Pattern objPattern = Pattern.compile("\\{[^\\{]+?\\}", Pattern.DOTALL);
        Matcher objMatcher = objPattern.matcher(text);

        while (objMatcher.find()) {
            String obj = objMatcher.group();

            String studentId = extractString(obj, "student_id");
            String card = extractString(obj, "card");
            String idType = extractString(obj, "id_type");

            if (studentId == null || studentId.isEmpty()) continue;

            Map<String, String> info = new LinkedHashMap<String, String>();
            String zjType = StudentInfoEnricher.toSpssZjtype(idType);
            StudentInfoEnricher.putDocumentNumber(info, zjType, card);
            if (zjType != null) info.put("ZJTYPE", zjType);
            studentInfo.put(studentId, info);
        }

        StudentInfoEnricher.addDocumentMappings(mappings, -1L);
        return new LoadResult(studentInfo, mappings);
    }

    public static void enrichRows(List<RowContext> rows, Map<String, Map<String, String>> studentInfo) {
        for (RowContext row : rows) {
            Map<String, String> info = studentInfo.get(row.getSampleKey());
            if (info != null) {
                for (Map.Entry<String, String> e : info.entrySet()) {
                    row.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    private static String extractString(String obj, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(obj);
        if (m.find()) return m.group(1).replace("\\/", "/").replace("\\\"", "\"");

        Pattern numP = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        m = numP.matcher(obj);
        if (m.find()) return m.group(1);

        return null;
    }

    public static class LoadResult {
        public final Map<String, Map<String, String>> studentInfo;
        public final Map<String, QuestionMapping> mappings;

        LoadResult(Map<String, Map<String, String>> studentInfo, Map<String, QuestionMapping> mappings) {
            this.studentInfo = studentInfo;
            this.mappings = mappings;
        }
    }
}
