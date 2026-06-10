package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将原来的字段级校验结果转换成页面需要的学生级结果。
 *
 * 页面不再直接展示 ERROR/WARN 字段表，而是展示：
 * 1. passedList：无任何校验问题的学生
 * 2. failedList：存在校验问题的学生，并列出违反规则
 */
public class StudentValidationResultBuilder {

    private final JdbcTemplate jdbc;

    public StudentValidationResultBuilder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> build(PrototypeFileReaders.AnswerCsvLoadResult loadResult,
                                     AnswerDataValidationReport report) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        List<AnswerRecord> answers = loadResult == null ? new ArrayList<AnswerRecord>() : loadResult.getAnswers();
        Map<String, StudentBucket> students = collectStudents(answers, loadResult);
        Map<Integer, StudentBucket> rowIndex = indexByRow(answers, students);
        Map<Long, String> namesFromDb = findStudentNames(collectPositiveStudentIds(students.values()));
        applyDbNames(students.values(), namesFromDb);

        if (report != null) {
            for (AnswerDataValidationIssue issue : report.getIssues()) {
                StudentBucket bucket = findBucketForIssue(issue, students, rowIndex);
                if (bucket == null) {
                    bucket = buildUnknownBucket(issue);
                    students.put(bucket.key, bucket);
                }
                bucket.violations.add(toViolation(issue));
            }
        }

        List<Map<String, Object>> passedList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> failedList = new ArrayList<Map<String, Object>>();
        int violationCount = 0;

        for (StudentBucket bucket : students.values()) {
            if (bucket.violations.isEmpty()) {
                passedList.add(bucket.toPassedMap());
            } else {
                violationCount += bucket.violations.size();
                failedList.add(bucket.toFailedMap());
            }
        }

        result.put("totalRows", report == null ? answers.size() : report.getTotalRows());
        result.put("studentCount", students.size());
        result.put("passedCount", passedList.size());
        result.put("failedCount", failedList.size());
        result.put("violationCount", violationCount);
        result.put("errorCount", report == null ? 0 : report.getErrorCount());
        result.put("warnCount", report == null ? 0 : report.getWarnCount());
        result.put("passed", report == null || report.isPassed());
        result.put("passedList", passedList);
        result.put("failedList", failedList);
        return result;
    }

    private Map<String, StudentBucket> collectStudents(List<AnswerRecord> answers,
                                                       PrototypeFileReaders.AnswerCsvLoadResult loadResult) {
        Map<String, StudentBucket> map = new LinkedHashMap<String, StudentBucket>();
        Map<String, String> namesFromCsv = loadResult == null ? new LinkedHashMap<String, String>() : loadResult.getStudentNamesByKey();
        for (AnswerRecord answer : answers) {
            String key = studentKey(answer.getStudentId(), answer.getSampleKey());
            StudentBucket bucket = map.get(key);
            if (bucket == null) {
                bucket = new StudentBucket();
                bucket.key = key;
                bucket.studentId = answer.getStudentId() > 0 ? Long.valueOf(answer.getStudentId()) : null;
                bucket.studentKey = answer.getSampleKey();
                bucket.studentName = namesFromCsv.get(answer.getSampleKey());
                map.put(key, bucket);
            }
        }
        return map;
    }

    private Map<Integer, StudentBucket> indexByRow(List<AnswerRecord> answers, Map<String, StudentBucket> students) {
        Map<Integer, StudentBucket> index = new HashMap<Integer, StudentBucket>();
        for (AnswerRecord answer : answers) {
            StudentBucket bucket = students.get(studentKey(answer.getStudentId(), answer.getSampleKey()));
            if (bucket != null && answer.getRowNumber() > 0) {
                index.put(Integer.valueOf(answer.getRowNumber()), bucket);
            }
        }
        return index;
    }

    private StudentBucket findBucketForIssue(AnswerDataValidationIssue issue,
                                             Map<String, StudentBucket> students,
                                             Map<Integer, StudentBucket> rowIndex) {
        if (issue == null) return null;
        String key = studentKey(issue.getStudentId() == null ? -1L : issue.getStudentId().longValue(), issue.getSampleKey());
        StudentBucket bucket = students.get(key);
        if (bucket != null) return bucket;
        if (issue.getRowNo() > 0) return rowIndex.get(Integer.valueOf(issue.getRowNo()));
        return null;
    }

    private StudentBucket buildUnknownBucket(AnswerDataValidationIssue issue) {
        StudentBucket bucket = new StudentBucket();
        bucket.studentId = issue == null ? null : issue.getStudentId();
        bucket.studentKey = issue == null ? "未知学生" : firstNonBlank(issue.getSampleKey(), issue.getRowNo() > 0 ? "第" + issue.getRowNo() + "行" : "未知学生");
        bucket.studentName = "";
        bucket.key = studentKey(bucket.studentId == null ? -1L : bucket.studentId.longValue(), bucket.studentKey);
        return bucket;
    }

    private Map<String, Object> toViolation(AnswerDataValidationIssue issue) {
        Map<String, Object> v = new LinkedHashMap<String, Object>();
        v.put("level", issue.getLevel());
        v.put("ruleCode", issue.getCode());
        v.put("ruleName", ruleName(issue.getCode()));
        v.put("message", issue.getMessage());
        v.put("suggestion", issue.getSuggestion());
        v.put("lineNo", issue.getRowNo());
        v.put("studentId", issue.getStudentId());
        v.put("sampleKey", issue.getSampleKey());
        v.put("questionId", issue.getQuestionId());
        v.put("optionId", issue.getOptionId());
        v.put("fieldName", issue.getFieldName());
        v.put("fieldValue", issue.getFieldValue());
        return v;
    }

    private String studentKey(long studentId, String sampleKey) {
        if (studentId > 0) return "ID:" + studentId;
        if (sampleKey != null && sampleKey.trim().length() > 0) return "KEY:" + sampleKey.trim();
        return "KEY:UNKNOWN";
    }

    private Set<Long> collectPositiveStudentIds(Collection<StudentBucket> buckets) {
        Set<Long> ids = new LinkedHashSet<Long>();
        for (StudentBucket bucket : buckets) {
            if (bucket.studentId != null && bucket.studentId.longValue() > 0) ids.add(bucket.studentId);
        }
        return ids;
    }

    private void applyDbNames(Collection<StudentBucket> buckets, Map<Long, String> namesFromDb) {
        for (StudentBucket bucket : buckets) {
            if (isBlank(bucket.studentName) && bucket.studentId != null) {
                String name = namesFromDb.get(bucket.studentId);
                if (!isBlank(name)) bucket.studentName = name;
            }
        }
    }

    private Map<Long, String> findStudentNames(Set<Long> studentIds) {
        Map<Long, String> result = new LinkedHashMap<Long, String>();
        if (jdbc == null || studentIds == null || studentIds.isEmpty()) return result;
        try {
            if (!tableExists("bus_student")) return result;
            String nameColumn = firstExistingColumn("bus_student", new String[]{"student_name", "name", "real_name", "xm", "studentName", "student_name_cn"});
            if (nameColumn == null) return result;
            List<Long> ids = new ArrayList<Long>(studentIds);
            for (List<Long> batch : batches(ids, 800)) {
                String delFilter = columnExists("bus_student", "del_flag") ? " AND del_flag = '0'" : "";
                String sql = "SELECT student_id, `" + nameColumn + "` AS student_name FROM bus_student WHERE student_id IN (" + placeholders(batch.size()) + ")" + delFilter;
                List<Map<String, Object>> rows = jdbc.queryForList(sql, batch.toArray());
                for (Map<String, Object> row : rows) {
                    Long id = asLongObj(row.get("student_id"));
                    String name = row.get("student_name") == null ? "" : String.valueOf(row.get("student_name"));
                    if (id != null && !isBlank(name)) result.put(id, name);
                }
            }
        } catch (Exception ignored) {
            // 学生姓名不是阻断项。查询失败时保留 CSV 中的姓名或空姓名。
        }
        return result;
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = database() AND table_name = ?",
                    new Object[]{tableName}, Integer.class);
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = database() AND table_name = ? AND column_name = ?",
                    new Object[]{tableName, columnName}, Integer.class);
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String firstExistingColumn(String tableName, String[] columns) {
        for (String column : columns) {
            if (columnExists(tableName, column)) return column;
        }
        return null;
    }

    private List<List<Long>> batches(List<Long> ids, int batchSize) {
        List<List<Long>> result = new ArrayList<List<Long>>();
        List<Long> current = new ArrayList<Long>();
        for (Long id : ids) {
            current.add(id);
            if (current.size() >= batchSize) {
                result.add(current);
                current = new ArrayList<Long>();
            }
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    private String placeholders(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private Long asLongObj(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return Long.valueOf(((Number) value).longValue());
        try {
            String s = String.valueOf(value).trim();
            if (s.length() == 0) return null;
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String a, String b) {
        if (!isBlank(a)) return a.trim();
        return isBlank(b) ? "" : b.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String ruleName(String code) {
        if (code == null) return "未知规则";
        Map<String, String> map = RULE_NAME_MAP;
        String name = map.get(code);
        return name == null ? code : name;
    }

    private static final Map<String, String> RULE_NAME_MAP = buildRuleNameMap();

    private static Map<String, String> buildRuleNameMap() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("EMPTY_LOAD_RESULT", "数据读取失败");
        map.put("EMPTY_DATA", "空数据检查");
        map.put("EMPTY_FILE", "空文件检查");
        map.put("MISSING_HEADER", "表头缺失检查");
        map.put("MISSING_REQUIRED_COLUMN", "必需字段缺失检查");
        map.put("BAD_ROW", "CSV行解析检查");
        map.put("BAD_QUESTION_ID", "问题ID格式检查");
        map.put("BAD_TABLE_ID", "表ID格式检查");
        map.put("MISSING_STUDENT", "学生标识缺失检查");
        map.put("MISSING_PROJECT_ID", "项目ID缺失检查");
        map.put("MISSING_TABLE_ID", "表ID缺失检查");
        map.put("MISSING_QUESTION_ID", "问题ID缺失检查");
        map.put("BAD_YEAR", "年份格式检查");
        map.put("INACTIVE_ROW", "删除标志检查");
        map.put("MIXED_TABLE_ID", "混合表ID检查");
        map.put("MIXED_PROJECT_ID", "混合项目ID检查");
        map.put("MIXED_YEAR", "混合年份检查");
        map.put("QUESTION_META_UNAVAILABLE", "问题字典读取检查");
        map.put("OPTION_META_UNAVAILABLE", "选项字典读取检查");
        map.put("QUESTION_NOT_FOUND", "问题存在性检查");
        map.put("QUESTION_DELETED", "问题有效性检查");
        map.put("QUESTION_TABLE_MISMATCH", "题目与表匹配检查");
        map.put("REQUIRED_CONTENT_EMPTY", "必填内容缺失检查");
        map.put("OPTION_ID_MISSING", "选项ID缺失检查");
        map.put("OPTION_NOT_FOUND", "选项存在性检查");
        map.put("OPTION_QUESTION_MISMATCH", "选项与题目匹配检查");
        map.put("OPTION_TABLE_MISMATCH", "选项与表匹配检查");
        map.put("OPTION_DELETED", "选项有效性检查");
        map.put("CONTENT_OPTION_CODE_MISMATCH", "答案内容与选项编码匹配检查");
        map.put("DUPLICATE_SAME_OPTION", "同选项重复数据检查");
        map.put("DUPLICATE_QUESTION", "同题重复数据检查");
        map.put("MISSING_REQUIRED_QUESTIONS", "学生必填题覆盖率检查");
        return map;
    }

    private static class StudentBucket {
        String key;
        Long studentId;
        String studentKey;
        String studentName;
        List<Map<String, Object>> violations = new ArrayList<Map<String, Object>>();

        Map<String, Object> toPassedMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("studentId", studentId);
            map.put("studentKey", studentKey);
            map.put("studentName", studentName == null ? "" : studentName);
            return map;
        }

        Map<String, Object> toFailedMap() {
            Map<String, Object> map = toPassedMap();
            map.put("violations", violations);
            map.put("violationCount", violations.size());
            map.put("ruleNames", joinViolationField("ruleName"));
            map.put("messages", joinViolationField("message"));
            return map;
        }

        private String joinViolationField(String field) {
            StringBuilder sb = new StringBuilder();
            Set<String> seen = new LinkedHashSet<String>();
            for (Map<String, Object> violation : violations) {
                Object value = violation.get(field);
                if (value == null) continue;
                String s = String.valueOf(value).trim();
                if (s.length() == 0 || seen.contains(s)) continue;
                if (sb.length() > 0) sb.append('；');
                sb.append(s);
                seen.add(s);
            }
            return sb.toString();
        }
    }
}
