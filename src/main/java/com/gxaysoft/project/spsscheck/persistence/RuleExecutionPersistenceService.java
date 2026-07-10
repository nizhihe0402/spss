package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.execution.DbRuleExecutionDataLoader;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RuleExecutionPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(RuleExecutionPersistenceService.class);

    private final JdbcTemplate jdbc;

    @Autowired
    private RuleExecutionPersistenceService self; // 自注入，用于触发 @Transactional AOP 代理

    public RuleExecutionPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SaveSummary saveDbExecutionResult(DbRuleExecutionDataLoader.Request request,
                                             String answerTable,
                                             PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                             Map<String, Object> spssResult) {
        return saveDbExecutionResult(request, answerTable, csvLoad, spssResult,
                Collections.<String, Map<String, String>>emptyMap());
    }

    /**
     * 保存执行结果到 _clean / _fail 表。
     * DDL（建表）与 DML（写数据）分离：MySQL 中 DDL 触发隐式提交会打断事务管理。
     */
    public SaveSummary saveDbExecutionResult(DbRuleExecutionDataLoader.Request request,
                                             String answerTable,
                                             PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                             Map<String, Object> spssResult,
                                             Map<String, Map<String, String>> correctionCleanValues) {
        String cleanTable = cleanTableName(answerTable);
        String failTable = failTableName(answerTable);

        log.info("开始保存执行结果: answerTable={}, cleanTable={}, failTable={}", answerTable, cleanTable, failTable);

        // Step 1: 确保表存在（DDL，不能在事务内执行）
        ensureCleanTable(answerTable, cleanTable);
        ensureFailTable(answerTable, failTable);

        // Step 2: 写入数据（DML，独立事务，通过 self 触发 AOP 代理）
        return self.doSaveData(request, answerTable, csvLoad, spssResult, correctionCleanValues,
                cleanTable, failTable);
    }

    @Transactional
    public SaveSummary doSaveData(DbRuleExecutionDataLoader.Request request,
                                   String answerTable,
                                   PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                   Map<String, Object> spssResult,
                                   Map<String, Map<String, String>> correctionCleanValues,
                                   String cleanTable, String failTable) {
        long cleanTaskId = System.currentTimeMillis();

        List<Long> cleanIds = cleanSourceIds(csvLoad, spssResult);
        List<FailDetail> failures = failDetails(spssResult);
        log.info("清洗统计: answers={}, cleanIds={}, failures={}",
                csvLoad != null && csvLoad.getAnswers() != null ? csvLoad.getAnswers().size() : 0,
                cleanIds.size(), failures.size());

        clearPreviousResults(request, answerTable, cleanTable, failTable, cleanIds);

        int cleanRows = insertCleanRows(answerTable, cleanTable, cleanIds, cleanTaskId);
        int correctionRows = updateCleanCorrections(request, cleanTable, csvLoad, correctionCleanValues);
        int failRows = insertFailRows(request, answerTable, failTable, failures, csvLoad, cleanTaskId);

        log.info("执行结果已保存: cleanTable={}, cleanRows={}, failRows={}, correctionRows={}",
                cleanTable, cleanRows, failRows, correctionRows);
        return new SaveSummary(cleanTable, failTable, cleanRows, failRows, correctionRows, cleanTaskId);
    }

    public static String cleanTableName(String answerTable) {
        assertSafeTableName(answerTable);
        return answerTable + "_clean";
    }

    public static String failTableName(String answerTable) {
        assertSafeTableName(answerTable);
        return answerTable + "_fail";
    }

    public static List<Long> cleanSourceIds(PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                            Map<String, Object> spssResult) {
        Set<String> passedKeys = studentKeys(asList(spssResult == null ? null : spssResult.get("passedList")));
        if (csvLoad == null || passedKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        for (AnswerRecord answer : csvLoad.getAnswers()) {
            if (answer == null || answer.getRawId() <= 0) continue;
            if (passedKeys.contains(answer.getSampleKey())) {
                result.add(Long.valueOf(answer.getRawId()));
            }
        }
        return result;
    }

    public static List<Long> allSourceIds(PrototypeFileReaders.AnswerCsvLoadResult csvLoad) {
        if (csvLoad == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        for (AnswerRecord answer : csvLoad.getAnswers()) {
            if (answer == null || answer.getRawId() <= 0) continue;
            result.add(Long.valueOf(answer.getRawId()));
        }
        return result;
    }

    public static List<FailDetail> failDetails(Map<String, Object> spssResult) {
        List<Map<String, Object>> failedStudents = asList(spssResult == null ? null : spssResult.get("failedList"));
        List<FailDetail> result = new ArrayList<FailDetail>();
        for (Map<String, Object> student : failedStudents) {
            String studentKey = stringValue(student.get("studentKey"));
            Long studentId = asLongObject(student.get("studentId"));
            List<Map<String, Object>> failedRules = asList(student.get("failedRules"));
            for (Map<String, Object> rule : failedRules) {
                result.add(new FailDetail(
                        studentKey,
                        studentId,
                        stringValue(rule.get("ruleCode")),
                        stringValue(rule.get("target")),
                        firstNotBlank(stringValue(rule.get("description")), stringValue(rule.get("displayText"))),
                        stringValue(rule.get("value")),
                        firstNotBlank(stringValue(rule.get("reason")), stringValue(student.get("failedDetailText"))),
                        stringValue(rule.get("displayText"))));
            }
        }
        return result;
    }

    private void ensureCleanTable(String answerTable, String cleanTable) {
        if (!tableExists(cleanTable)) {
            jdbc.execute("CREATE TABLE " + cleanTable + " LIKE " + answerTable);
        }
        ensureColumn(cleanTable, "is_valid", "TINYINT DEFAULT 1");
        ensureColumn(cleanTable, "invalid_reason", "JSON NULL");
        ensureColumn(cleanTable, "clean_task_id", "BIGINT NULL");
        ensureColumn(cleanTable, "clean_time", "DATETIME NULL");
        ensureColumn(cleanTable, "source_id", "BIGINT NULL");
    }

    /**
     * 创建 _fail 表：镜像源表结构 + 错误追踪元数据列。
     * 与 _clean 表策略一致（CREATE TABLE ... LIKE 拷贝源表全部列）。
     */
    private void ensureFailTable(String answerTable, String failTable) {
        if (!tableExists(failTable)) {
            jdbc.execute("CREATE TABLE " + failTable + " LIKE " + answerTable);
        }
        ensureColumn(failTable, "clean_task_id", "BIGINT NULL");
        ensureColumn(failTable, "source_id", "BIGINT NULL");
        ensureColumn(failTable, "division_id", "BIGINT NULL COMMENT '区县(执行参数)'");
        ensureColumn(failTable, "school_id", "BIGINT NULL COMMENT '学校(执行参数)'");
        ensureColumn(failTable, "rule_code", "VARCHAR(50) NULL");
        ensureColumn(failTable, "rule_target", "VARCHAR(100) NULL");
        ensureColumn(failTable, "rule_name", "VARCHAR(300) NULL");
        ensureColumn(failTable, "failed_value", "VARCHAR(500) NULL");
        ensureColumn(failTable, "reason", "VARCHAR(500) NULL");
        ensureColumn(failTable, "reason_detail", "LONGTEXT NULL");
    }

    private void clearPreviousResults(DbRuleExecutionDataLoader.Request request,
                                      String answerTable,
                                      String cleanTable,
                                      String failTable,
                                      List<Long> cleanIds) {
        // _clean: 按 source_id 精确删除已清洗的记录
        if (!cleanIds.isEmpty()) {
            jdbc.update("DELETE FROM " + cleanTable + " WHERE source_id IN (" + placeholders(cleanIds.size()) + ")",
                    cleanIds.toArray());
        }
        // _fail: 按执行范围删除（project/table/year/division/school），source_id 无法穷举
        if (hasTimesColumn(answerTable) || isStudentAnswer(answerTable)) {
            // 医生/学生表：可按区县+学校过滤
            jdbc.update("DELETE FROM " + failTable + " WHERE project_id=? AND table_id=? AND year=? " +
                            "AND division_id=? AND (? <= 0 OR school_id=?)",
                    request.projectId, request.tableId, request.year,
                    request.divisionId, request.schoolId, request.schoolId);
        } else {
            // 用户表：无区县/学校维度，按 project/table/year 过滤
            jdbc.update("DELETE FROM " + failTable + " WHERE project_id=? AND table_id=? AND year=?",
                    request.projectId, request.tableId, request.year);
        }
    }

    private int insertCleanRows(String answerTable, String cleanTable, List<Long> cleanIds, long cleanTaskId) {
        if (cleanIds.isEmpty()) {
            return 0;
        }
        String columns = baseColumns(answerTable);
        String selectColumns = selectedBaseColumns(answerTable);
        String sql = "INSERT INTO " + cleanTable + " (" + columns +
                ", is_valid, invalid_reason, clean_task_id, clean_time, source_id) " +
                "SELECT " + selectColumns + ", 1, NULL, ?, NOW(), a.id FROM " + answerTable + " a " +
                "WHERE a.id IN (" + placeholders(cleanIds.size()) + ")";
        List<Object> args = new ArrayList<Object>();
        args.add(Long.valueOf(cleanTaskId));
        args.addAll(cleanIds);
        return jdbc.update(sql, args.toArray());
    }

    private int updateCleanCorrections(DbRuleExecutionDataLoader.Request request,
                                       String cleanTable,
                                       PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                       Map<String, Map<String, String>> correctionCleanValues) {
        if (csvLoad == null || correctionCleanValues == null || correctionCleanValues.isEmpty()) {
            return 0;
        }
        Map<Long, String> questionVariables = loadQuestionVariables(request.scriptId);
        int count = 0;
        for (AnswerRecord answer : csvLoad.getAnswers()) {
            if (answer == null || answer.getRawId() <= 0) continue;
            Map<String, String> rowCorrections = correctionCleanValues.get(answer.getSampleKey());
            if (rowCorrections == null || rowCorrections.isEmpty()) continue;
            String variable = questionVariables.get(Long.valueOf(answer.getQuestionId()));
            if (variable == null) continue;
            String corrected = rowCorrections.get(variable.trim().toUpperCase());
            if (corrected == null) continue;
            count += jdbc.update("UPDATE " + cleanTable + " SET content=? WHERE source_id=?",
                    corrected, Long.valueOf(answer.getRawId()));
        }
        return count;
    }

    private Map<Long, String> loadQuestionVariables(long scriptId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT question_id, variable_name FROM sps_script_question_mapping WHERE script_id=?",
                Long.valueOf(scriptId));
        Map<Long, String> result = new LinkedHashMap<Long, String>();
        for (Map<String, Object> row : rows) {
            Long questionId = asLongObject(row.get("question_id"));
            if (questionId == null) continue;
            String variable = stringValue(row.get("variable_name"));
            if (variable.isEmpty()) continue;
            result.put(questionId, variable.trim().toUpperCase());
        }
        return result;
    }

    /**
     * 插入失败记录到 _fail 表：从源表复制完整行数据 + 错误元数据。
     * 新 _fail 表结构镜像源表（CREATE TABLE ... LIKE），故用 INSERT ... SELECT。
     */
    private int insertFailRows(DbRuleExecutionDataLoader.Request request,
                               String answerTable,
                               String failTable,
                               List<FailDetail> failures,
                               PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                               long cleanTaskId) {
        if (failures.isEmpty()) {
            return 0;
        }
        Map<String, AnswerRecord> sourceByStudent = firstAnswerByStudent(csvLoad);
        String columns = baseColumns(answerTable);
        String selectColumns = selectedBaseColumns(answerTable);
        String sql = "INSERT INTO " + failTable + " (" + columns +
                ", clean_task_id, source_id, division_id, school_id, " +
                "rule_code, rule_target, rule_name, failed_value, reason, reason_detail) " +
                "SELECT " + selectColumns + ", ?, a.id, ?, ?, ?, ?, ?, ?, ? " +
                "FROM " + answerTable + " a WHERE a.id = ?";
        int count = 0;
        for (FailDetail failure : failures) {
            AnswerRecord source = sourceByStudent.get(failure.studentKey);
            Long sourceId = source == null || source.getRawId() <= 0 ? null : Long.valueOf(source.getRawId());
            if (sourceId == null) continue;
            jdbc.update(sql,
                    Long.valueOf(cleanTaskId),
                    Long.valueOf(request.divisionId),
                    request.schoolId > 0 ? Long.valueOf(request.schoolId) : null,
                    truncate(failure.ruleCode, 50),
                    truncate(failure.ruleTarget, 100),
                    truncate(failure.ruleName, 300),
                    truncate(failure.failedValue, 500),
                    truncate(failure.reasonDetail, 500),
                    failure.reasonDetail,
                    sourceId);
            count++;
        }
        return count;
    }

    private static String baseColumns(String answerTable) {
        if (isUserAnswer(answerTable)) {
            return "question_id, option_id, code, content, project_id, table_id, year, del_flag, " +
                    "create_by, create_time, update_by, update_time, remark";
        }
        if (hasTimesColumn(answerTable)) {
            return "question_id, option_id, student_id, content, project_id, table_id, times, year, del_flag, " +
                    "create_by, create_time, update_by, update_time, remark";
        }
        return "question_id, option_id, student_id, content, project_id, table_id, year, del_flag, " +
                "create_by, create_time, update_by, update_time, remark";
    }

    private static String selectedBaseColumns(String answerTable) {
        if (isUserAnswer(answerTable)) {
            return "a.question_id, a.option_id, a.code, a.content, a.project_id, a.table_id, a.year, a.del_flag, " +
                    "a.create_by, a.create_time, a.update_by, a.update_time, a.remark";
        }
        if (hasTimesColumn(answerTable)) {
            return "a.question_id, a.option_id, a.student_id, a.content, a.project_id, a.table_id, a.times, a.year, a.del_flag, " +
                    "a.create_by, a.create_time, a.update_by, a.update_time, a.remark";
        }
        return "a.question_id, a.option_id, a.student_id, a.content, a.project_id, a.table_id, a.year, a.del_flag, " +
                "a.create_by, a.create_time, a.update_by, a.update_time, a.remark";
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                Integer.class, tableName);
        return count != null && count.intValue() > 0;
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?",
                Integer.class, tableName, columnName);
        if (count == null || count.intValue() == 0) {
            jdbc.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private static Map<String, AnswerRecord> firstAnswerByStudent(PrototypeFileReaders.AnswerCsvLoadResult csvLoad) {
        Map<String, AnswerRecord> result = new LinkedHashMap<String, AnswerRecord>();
        if (csvLoad == null) return result;
        for (AnswerRecord answer : csvLoad.getAnswers()) {
            if (answer == null || isBlank(answer.getSampleKey()) || result.containsKey(answer.getSampleKey())) continue;
            result.put(answer.getSampleKey(), answer);
        }
        return result;
    }

    private static Set<String> studentKeys(List<Map<String, Object>> students) {
        Set<String> keys = new LinkedHashSet<String>();
        for (Map<String, Object> student : students) {
            String key = stringValue(student.get("studentKey"));
            if (!isBlank(key)) keys.add(key);
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : raw) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private static String placeholders(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private static boolean isUserAnswer(String answerTable) {
        return answerTable.startsWith("bus_user_answer");
    }

    private static boolean isStudentAnswer(String answerTable) {
        return answerTable.startsWith("bus_student_answer");
    }

    private static boolean hasTimesColumn(String answerTable) {
        return answerTable.startsWith("bus_doctor_answer");
    }

    private static Long asLongObject(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return Long.valueOf(((Number) value).longValue());
        try {
            String text = String.valueOf(value).trim();
            if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
            return text.isEmpty() ? null : Long.valueOf(Long.parseLong(text));
        } catch (Exception e) {
            log.warn("类型转换失败: {}", e.getMessage());
            return null;
        }
    }

    private static String firstNotBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void assertSafeTableName(String tableName) {
        if (tableName == null || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("非法数据表名: " + tableName);
        }
    }

    public static final class SaveSummary {
        private final String cleanTable;
        private final String failTable;
        private final int cleanRows;
        private final int failRows;
        private final int correctionRows;
        private final long cleanTaskId;

        SaveSummary(String cleanTable, String failTable, int cleanRows, int failRows, long cleanTaskId) {
            this(cleanTable, failTable, cleanRows, failRows, 0, cleanTaskId);
        }

        SaveSummary(String cleanTable, String failTable, int cleanRows, int failRows, int correctionRows, long cleanTaskId) {
            this.cleanTable = cleanTable;
            this.failTable = failTable;
            this.cleanRows = cleanRows;
            this.failRows = failRows;
            this.correctionRows = correctionRows;
            this.cleanTaskId = cleanTaskId;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("cleanTable", cleanTable);
            result.put("failTable", failTable);
            result.put("cleanRows", cleanRows);
            result.put("failRows", failRows);
            result.put("correctionRows", correctionRows);
            result.put("cleanTaskId", cleanTaskId);
            return result;
        }
    }

    public static final class FailDetail {
        public final String studentKey;
        public final Long studentId;
        public final String ruleCode;
        public final String ruleTarget;
        public final String ruleName;
        public final String failedValue;
        public final String reasonDetail;
        public final String displayText;

        FailDetail(String studentKey,
                   Long studentId,
                   String ruleCode,
                   String ruleTarget,
                   String ruleName,
                   String failedValue,
                   String reasonDetail,
                   String displayText) {
            this.studentKey = studentKey;
            this.studentId = studentId;
            this.ruleCode = ruleCode;
            this.ruleTarget = ruleTarget;
            this.ruleName = ruleName;
            this.failedValue = failedValue;
            this.reasonDetail = reasonDetail;
            this.displayText = displayText;
        }
    }
}
