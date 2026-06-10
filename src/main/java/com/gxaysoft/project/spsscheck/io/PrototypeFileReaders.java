package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrototypeFileReaders {
    private PrototypeFileReaders() {
    }

    public static String readSpssText(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return decodeTextAuto(bytes);
    }

    public static List<AnswerRecord> readAnswerCsv(Path csvPath) throws IOException {
        return readAnswerCsvDetailed(csvPath).getAnswers();
    }

    public static AnswerCsvLoadResult readAnswerCsvDetailed(Path csvPath) throws IOException {
        return readAnswerCsvDetailed(Files.readAllBytes(csvPath));
    }

    public static AnswerCsvLoadResult readAnswerCsvDetailed(byte[] csvBytes) {
        String text = decodeTextAuto(csvBytes);
        return parseAnswerCsvText(text);
    }

    public static String decodeTextAuto(byte[] bytes) {
        if (bytes == null) return "";
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            return new String(bytes, Charset.forName("GB18030"));
        }
    }

    private static AnswerCsvLoadResult parseAnswerCsvText(String text) {
        AnswerCsvLoadResult result = new AnswerCsvLoadResult();
        if (text == null || text.trim().isEmpty()) {
            result.addParseWarning(1, "EMPTY_FILE", "CSV 文件为空");
            return result;
        }

        String[] lines = text.split("\\r?\\n", -1);
        if (lines.length == 0 || lines[0].trim().isEmpty()) {
            result.addParseWarning(1, "MISSING_HEADER", "CSV 缺少表头");
            return result;
        }

        List<String> headers = parseDoubleQuotedCsv(lines[0]);
        List<String> normalizedHeaders = new ArrayList<>();
        for (String h : headers) {
            normalizedHeaders.add(cleanCell(h).toLowerCase(Locale.ROOT));
        }
        result.headers.addAll(normalizedHeaders);

        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < normalizedHeaders.size(); i++) {
            index.put(normalizedHeaders.get(i), i);
        }

        requireHeader(result, index, "question_id");
        requireHeader(result, index, "content");
        if (!index.containsKey("student_id") && !index.containsKey("code")) {
            result.addParseWarning(1, "MISSING_REQUIRED_COLUMN", "CSV 缺少 student_id 或 code 字段，无法按学生透视执行规则");
        }
        requireHeader(result, index, "table_id");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isEmpty()) continue;
            int rowNo = i + 1;
            List<String> values = parseDoubleQuotedCsv(line);
            try {
                String sampleKey = firstNonBlank(get(values, index.get("code")), get(values, index.get("student_id")));
                String studentName = firstNonBlank(
                        get(values, firstExistingIndex(index, "student_name", "studentname", "name", "real_name", "xm", "姓名")),
                        get(values, firstExistingIndex(index, "student_name_cn", "student_name_zh", "学生姓名"))
                );
                if (sampleKey != null && !sampleKey.trim().isEmpty() && studentName != null && !studentName.trim().isEmpty()) {
                    result.studentNamesByKey.put(sampleKey, studentName);
                }
                long rawId = parseLong(get(values, index.get("id")), -1L);
                long questionId = parseLong(get(values, index.get("question_id")), -1L);
                long optionId = parseLong(get(values, index.get("option_id")), 0L);
                long studentId = parseLong(get(values, index.get("student_id")), -1L);
                String content = cleanCell(get(values, index.get("content")));
                long projectId = parseLong(get(values, index.get("project_id")), -1L);
                long tableId = parseLong(get(values, index.get("table_id")), -1L);
                String times = cleanCell(get(values, index.get("times")));
                String year = cleanCell(get(values, index.get("year")));
                String delFlag = cleanCell(get(values, index.get("del_flag")));

                if (questionId <= 0) {
                    result.addParseWarning(rowNo, "BAD_QUESTION_ID", "question_id 为空或不是有效数字");
                }
                if (tableId <= 0) {
                    result.addParseWarning(rowNo, "BAD_TABLE_ID", "table_id 为空或不是有效数字");
                }
                result.answers.add(new AnswerRecord(rawId, rowNo, sampleKey, questionId, optionId, studentId,
                        content, projectId, tableId, times, year, delFlag));
            } catch (Exception ex) {
                result.addParseWarning(rowNo, "BAD_ROW", "CSV 第 " + rowNo + " 行解析失败: " + ex.getMessage());
            }
        }
        return result;
    }

    private static void requireHeader(AnswerCsvLoadResult result, Map<String, Integer> index, String name) {
        if (!index.containsKey(name)) {
            result.addParseWarning(1, "MISSING_REQUIRED_COLUMN", "CSV 缺少必需字段: " + name);
        }
    }

    private static String get(List<String> values, Integer idx) {
        if (idx == null || idx < 0 || idx >= values.size()) return null;
        return values.get(idx);
    }

    private static long parseLong(String value, long defaultValue) {
        String v = cleanCell(value);
        if (v == null || v.isEmpty()) return defaultValue;
        try {
            if (v.endsWith(".0")) v = v.substring(0, v.length() - 2);
            return Long.parseLong(v);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String a, String b) {
        String aa = cleanCell(a);
        if (aa != null && !aa.isEmpty()) return aa;
        String bb = cleanCell(b);
        return bb == null ? "" : bb;
    }

    private static Integer firstExistingIndex(Map<String, Integer> index, String... names) {
        if (index == null || names == null) return null;
        for (String name : names) {
            if (name == null) continue;
            Integer idx = index.get(name.toLowerCase(Locale.ROOT));
            if (idx != null) return idx;
        }
        return null;
    }

    private static String cleanCell(String value) {
        if (value == null) return null;
        String v = value.replace("\uFEFF", "").replace("\t", "").trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private static List<String> parseDoubleQuotedCsv(String line) {
        if (line == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                values.add(cleanCell(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(cleanCell(current.toString()));
        return values;
    }

    public static class AnswerCsvLoadResult {
        private final List<String> headers = new ArrayList<>();
        private final List<AnswerRecord> answers = new ArrayList<>();
        private final List<ParseWarning> parseWarnings = new ArrayList<>();
        private final Map<String, String> studentNamesByKey = new LinkedHashMap<>();

        public List<String> getHeaders() {
            return headers;
        }

        public List<AnswerRecord> getAnswers() {
            return answers;
        }

        public List<ParseWarning> getParseWarnings() {
            return parseWarnings;
        }

        public Map<String, String> getStudentNamesByKey() {
            return studentNamesByKey;
        }

        public void addParseWarning(int rowNo, String code, String message) {
            parseWarnings.add(new ParseWarning(rowNo, code, message));
        }
    }

    public static class ParseWarning {
        private final int rowNo;
        private final String code;
        private final String message;

        public ParseWarning(int rowNo, String code, String message) {
            this.rowNo = rowNo;
            this.code = code;
            this.message = message;
        }

        public int getRowNo() {
            return rowNo;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
