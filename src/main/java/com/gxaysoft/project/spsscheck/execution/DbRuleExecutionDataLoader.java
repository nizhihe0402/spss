package com.gxaysoft.project.spsscheck.execution;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbRuleExecutionDataLoader {
    private final JdbcTemplate jdbc;

    public DbRuleExecutionDataLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LoadResult load(Request request) {
        request.validate();
        String answerTable = resolveAnswerTable(request.tableId, request.source);
        boolean userAnswer = isTableOne(request.tableId);
        String sql = buildSqlForTable(request.tableId, answerTable);
        Object[] args = userAnswer
                ? new Object[]{request.projectId, request.tableId, request.year, request.divisionId}
                : new Object[]{request.projectId, request.tableId, request.year, request.divisionId, request.schoolId};
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);

        PrototypeFileReaders.AnswerCsvLoadResult loadResult = new PrototypeFileReaders.AnswerCsvLoadResult();
        int rowNo = 1;
        for (Map<String, Object> row : rows) {
            AnswerRecord record = toAnswerRecord(row, userAnswer, rowNo++);
            loadResult.getAnswers().add(record);
            String studentName = stringValue(row.get("student_name"));
            if (!studentName.isEmpty()) {
                loadResult.getStudentNamesByKey().put(record.getSampleKey(), studentName);
            }
        }
        return new LoadResult(answerTable, loadResult);
    }

    public static String resolveAnswerTable(long tableId, String source) {
        String normalizedSource = normalizeSource(source);
        if (isTableOne(tableId)) {
            if ("intervene".equals(normalizedSource)) {
                throw new IllegalArgumentException("表1-X 暂不支持 source=intervene");
            }
            return "bus_user_answer";
        }
        if (isTableTwo(tableId)) {
            return "intervene".equals(normalizedSource) ? "bus_doctor_answer_intervene" : "bus_doctor_answer";
        }
        if (isTableThree(tableId)) {
            return "intervene".equals(normalizedSource) ? "bus_student_answer_intervene" : "bus_student_answer";
        }
        throw new IllegalArgumentException("不支持的 tableId: " + tableId);
    }

    public static AnswerRecord toAnswerRecord(Map<String, Object> row, boolean userAnswer, int rowNumber) {
        long rawId = asLong(row.get("id"), -1L);
        long questionId = asLong(row.get("question_id"), -1L);
        long optionId = asLong(row.get("option_id"), 0L);
        long sampleId = asLong(row.get(userAnswer ? "code" : "student_id"), -1L);
        String sampleKey = sampleId > 0 ? String.valueOf(sampleId) : stringValue(row.get(userAnswer ? "code" : "student_id"));
        return new AnswerRecord(
                rawId,
                rowNumber,
                sampleKey,
                questionId,
                optionId,
                sampleId,
                stringValue(row.get("content")),
                asLong(row.get("project_id"), -1L),
                asLong(row.get("table_id"), -1L),
                stringValue(row.get("times")),
                stringValue(row.get("year")),
                stringValue(row.get("del_flag"))
        );
    }

    public static String buildSqlForTable(long tableId, String answerTable) {
        return buildSql(answerTable, isTableOne(tableId), isTableTwo(tableId));
    }

    private static String buildSql(String answerTable, boolean userAnswer, boolean hasTimesColumn) {
        if (userAnswer) {
            return "SELECT a.id, a.question_id, a.option_id, a.code, a.content, a.project_id, a.table_id, " +
                    "NULL AS times, a.year, a.del_flag " +
                    "FROM " + answerTable + " a " +
                    "WHERE a.project_id=? AND a.table_id=? AND a.year=? AND a.del_flag='0' " +
                    "AND EXISTS (SELECT 1 FROM bus_user_answer_log l WHERE l.project_id=a.project_id " +
                    "AND l.table_id=a.table_id AND l.year=a.year AND l.code=CAST(a.code AS CHAR) " +
                    "AND l.division_id=? AND l.del_flag='0') " +
                    "ORDER BY a.code, a.question_id, a.id";
        }
        String timesSelect = hasTimesColumn ? "a.times" : "NULL AS times";
        return "SELECT a.id, a.question_id, a.option_id, a.student_id, a.content, a.project_id, a.table_id, " +
                timesSelect + ", a.year, a.del_flag, s.student_name " +
                "FROM " + answerTable + " a " +
                "JOIN bus_student s ON s.student_id=a.student_id " +
                "WHERE a.project_id=? AND a.table_id=? AND a.year=? AND a.del_flag='0' " +
                "AND s.division_id=? AND s.school_id=? AND s.del_flag='0' " +
                "ORDER BY a.student_id, a.question_id, a.id";
    }

    private static String normalizeSource(String source) {
        String value = source == null || source.trim().isEmpty() ? "normal" : source.trim().toLowerCase();
        if (!"normal".equals(value) && !"intervene".equals(value)) {
            throw new IllegalArgumentException("source 只支持 normal 或 intervene");
        }
        return value;
    }

    private static boolean isTableOne(long tableId) {
        return tableId == 1L || tableId == 2L || tableId == 10L;
    }

    private static boolean isTableTwo(long tableId) {
        return tableId == 3L || tableId == 4L || tableId == 5L;
    }

    private static boolean isTableThree(long tableId) {
        return tableId == 6L || tableId == 7L || tableId == 8L;
    }

    private static long asLong(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            String text = value == null ? "" : String.valueOf(value).trim();
            if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
            return text.isEmpty() ? fallback : Long.parseLong(text);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static class Request {
        public long scriptId;
        public long projectId;
        public long tableId;
        public long divisionId;
        public long schoolId;
        public String year;
        public String source;

        public void validate() {
            if (scriptId <= 0) throw new IllegalArgumentException("scriptId 必填");
            if (projectId <= 0) throw new IllegalArgumentException("projectId 必填");
            if (tableId <= 0) throw new IllegalArgumentException("tableId 必填");
            if (divisionId <= 0) throw new IllegalArgumentException("divisionId 必填");
            if (year == null || year.trim().isEmpty()) throw new IllegalArgumentException("year 必填");
            if ((isTableTwo(tableId) || isTableThree(tableId)) && schoolId <= 0) {
                throw new IllegalArgumentException("表2-X/表3-X 执行必须传 schoolId");
            }
        }
    }

    public static class LoadResult {
        private final String answerTable;
        private final PrototypeFileReaders.AnswerCsvLoadResult csvLoad;

        public LoadResult(String answerTable, PrototypeFileReaders.AnswerCsvLoadResult csvLoad) {
            this.answerTable = answerTable;
            this.csvLoad = csvLoad;
        }

        public String getAnswerTable() {
            return answerTable;
        }

        public PrototypeFileReaders.AnswerCsvLoadResult getCsvLoad() {
            return csvLoad;
        }
    }
}
