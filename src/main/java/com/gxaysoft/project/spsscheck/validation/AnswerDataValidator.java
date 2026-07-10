package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * Validates uploaded bus_doctor_answer style CSV before SPSS rule execution.
 *
 * Checks are intentionally placed before pivot/execution.  Otherwise bad table_id,
 * invalid option_id, deleted questions, or mixed project/year data can silently
 * produce false rule results.
 */
public class AnswerDataValidator {
    private final JdbcTemplate jdbc;

    public AnswerDataValidator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AnswerDataValidationReport validate(PrototypeFileReaders.AnswerCsvLoadResult loadResult) {
        return validate(loadResult, true);
    }

    public AnswerDataValidationReport validate(PrototypeFileReaders.AnswerCsvLoadResult loadResult,
                                               boolean enforceTableIdConsistency) {
        AnswerDataValidationReport report = new AnswerDataValidationReport();
        if (loadResult == null) {
            report.addError("EMPTY_LOAD_RESULT", 0, null, null, null, null,
                    null, null, "未读取到 CSV 数据", "请重新上传 bus_doctor_answer CSV 文件");
            return report;
        }
        List<AnswerRecord> answers = loadResult.getAnswers();
        report.setTotalRows(answers.size());

        for (PrototypeFileReaders.ParseWarning w : loadResult.getParseWarnings()) {
            report.addError(w.getCode(), w.getRowNo(), null, null, null, null,
                    null, null, w.getMessage(), "检查 CSV 表头、编码、数字字段和分隔符");
        }

        if (answers.isEmpty()) {
            report.addError("EMPTY_DATA", 0, null, null, null, null,
                    null, null, "CSV 中没有有效数据行", "请确认文件不是空表，并包含 question_id/content/table_id 等字段");
            return report;
        }

        basicFieldCheck(answers, report, enforceTableIdConsistency);
        Map<Long, QuestionMeta> questionMeta = loadQuestionMeta(answers, report);
        Map<Long, List<OptionMeta>> optionsByQuestion = loadOptionsByQuestion(answers, report);
        Map<Long, OptionMeta> optionsById = flattenOptions(optionsByQuestion);

        questionAndOptionCheck(answers, questionMeta, optionsByQuestion, optionsById, report, enforceTableIdConsistency);
        duplicateCheck(answers, questionMeta, report);
        coverageCheck(answers, questionMeta, report);

        return report;
    }

    private void basicFieldCheck(List<AnswerRecord> answers,
                                 AnswerDataValidationReport report,
                                 boolean enforceTableIdConsistency) {
        Set<Long> tableIds = new TreeSet<>();
        Set<Long> projectIds = new TreeSet<>();
        Set<String> years = new TreeSet<>();
        Set<String> delFlags = new TreeSet<>();
        Set<String> times = new TreeSet<>();
        Set<String> students = new TreeSet<>();

        for (AnswerRecord a : answers) {
            if (a.getTableId() > 0) tableIds.add(a.getTableId());
            if (a.getProjectId() > 0) projectIds.add(a.getProjectId());
            if (!isBlank(a.getYear())) years.add(a.getYear());
            if (!isBlank(a.getDelFlag())) delFlags.add(a.getDelFlag());
            if (!isBlank(a.getTimes())) times.add(a.getTimes());
            if (!isBlank(a.getSampleKey())) students.add(a.getSampleKey());

            if (a.getSampleKey() == null || a.getSampleKey().trim().isEmpty()) {
                report.addError("MISSING_STUDENT", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), "student_id/code", null,
                        "缺少学生标识，无法按学生聚合执行规则", "补充 student_id 或 code 字段");
            }
            if (a.getProjectId() <= 0) {
                report.addWarn("MISSING_PROJECT_ID", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), "project_id", String.valueOf(a.getProjectId()),
                        "project_id 为空或不是有效数字", "建议补齐 project_id，用于区分不同项目数据");
            }
            if (a.getTableId() <= 0) {
                addTableIdIssue(report, enforceTableIdConsistency, "MISSING_TABLE_ID",
                        a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), String.valueOf(a.getTableId()),
                        "table_id 为空或不是有效数字", "补充正确 table_id，否则无法确定使用哪张表的问题字典");
            }
            if (a.getQuestionId() <= 0) {
                report.addError("MISSING_QUESTION_ID", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), "question_id", String.valueOf(a.getQuestionId()),
                        "question_id 为空或不是有效数字", "补充正确 question_id");
            }
            if (!isBlank(a.getYear()) && !a.getYear().matches("^[0-9]{4}$")) {
                report.addWarn("BAD_YEAR", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), "year", a.getYear(),
                        "year 不是 4 位年份", "建议使用 2025 这种 4 位年份格式");
            }
            if (!isBlank(a.getDelFlag()) && !"0".equals(a.getDelFlag())) {
                report.addError("INACTIVE_ROW", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), "del_flag", a.getDelFlag(),
                        "del_flag 不是 0，属于删除/无效数据，不应参与规则执行", "执行前过滤 del_flag=0 的有效数据");
            }
        }

        report.getSummary().put("tableIds", new ArrayList<Long>(tableIds));
        report.getSummary().put("projectIds", new ArrayList<Long>(projectIds));
        report.getSummary().put("years", new ArrayList<String>(years));
        report.getSummary().put("delFlags", new ArrayList<String>(delFlags));
        report.getSummary().put("times", new ArrayList<String>(times));
        report.getSummary().put("studentCount", students.size());

        if (tableIds.size() > 1) {
            addTableIdIssue(report, enforceTableIdConsistency, "MIXED_TABLE_ID",
                    0, null, null, null, null, tableIds.toString(),
                    "同一个 CSV 中存在多个 table_id，执行规则时只能稳定匹配一张表", "按 table_id 拆分后分别执行，或确认脚本是否支持混合表");
        }
        if (projectIds.size() > 1) {
            report.addWarn("MIXED_PROJECT_ID", 0, null, null, null, null, "project_id", projectIds.toString(),
                    "同一个 CSV 中存在多个 project_id", "建议按 project_id 拆分，避免跨项目统计污染");
        }
        if (years.size() > 1) {
            report.addWarn("MIXED_YEAR", 0, null, null, null, null, "year", years.toString(),
                    "同一个 CSV 中存在多个年份", "建议按 year 拆分，避免跨年度规则或阈值混用");
        }
    }

    private Map<Long, QuestionMeta> loadQuestionMeta(List<AnswerRecord> answers, AnswerDataValidationReport report) {
        Set<Long> ids = new LinkedHashSet<>();
        for (AnswerRecord a : answers) if (a.getQuestionId() > 0) ids.add(a.getQuestionId());
        Map<Long, QuestionMeta> map = new LinkedHashMap<>();
        if (ids.isEmpty()) return map;
        try {
            for (List<Long> batch : batches(ids, 800)) {
                String sql = "SELECT question_id, table_id, content, type, bitian, del_flag FROM bus_question WHERE question_id IN (" + placeholders(batch.size()) + ")";
                List<Map<String, Object>> rows = jdbc.queryForList(sql, batch.toArray());
                for (Map<String, Object> r : rows) {
                    QuestionMeta q = new QuestionMeta();
                    q.questionId = asLong(r.get("question_id"));
                    q.tableId = asLong(r.get("table_id"));
                    q.content = asString(r.get("content"));
                    q.type = asString(r.get("type"));
                    q.bitian = asString(r.get("bitian"));
                    q.delFlag = asString(r.get("del_flag"));
                    map.put(q.questionId, q);
                }
            }
        } catch (Exception ex) {
            report.addWarn("QUESTION_META_UNAVAILABLE", 0, null, null, null, null, "bus_question", null,
                    "无法读取 bus_question 元数据: " + ex.getMessage(), "确认业务库中存在 bus_question 表，并且当前账号有查询权限");
        }
        return map;
    }

    private Map<Long, List<OptionMeta>> loadOptionsByQuestion(List<AnswerRecord> answers, AnswerDataValidationReport report) {
        Set<Long> qids = new LinkedHashSet<>();
        for (AnswerRecord a : answers) if (a.getQuestionId() > 0) qids.add(a.getQuestionId());
        Map<Long, List<OptionMeta>> byQuestion = new LinkedHashMap<>();
        if (qids.isEmpty()) return byQuestion;
        try {
            for (List<Long> batch : batches(qids, 800)) {
                String sql = "SELECT option_id, question_id, table_id, code, content, del_flag FROM bus_option WHERE question_id IN (" + placeholders(batch.size()) + ")";
                List<Map<String, Object>> rows = jdbc.queryForList(sql, batch.toArray());
                for (Map<String, Object> r : rows) {
                    OptionMeta o = new OptionMeta();
                    o.optionId = asLong(r.get("option_id"));
                    o.questionId = asLong(r.get("question_id"));
                    o.tableId = asLong(r.get("table_id"));
                    o.code = asString(r.get("code"));
                    o.content = asString(r.get("content"));
                    o.delFlag = asString(r.get("del_flag"));
                    List<OptionMeta> list = byQuestion.get(o.questionId);
                    if (list == null) {
                        list = new ArrayList<>();
                        byQuestion.put(o.questionId, list);
                    }
                    list.add(o);
                }
            }
        } catch (Exception ex) {
            report.addWarn("OPTION_META_UNAVAILABLE", 0, null, null, null, null, "bus_option", null,
                    "无法读取 bus_option 元数据: " + ex.getMessage(), "确认业务库中存在 bus_option 表，并且当前账号有查询权限");
        }
        return byQuestion;
    }

    private Map<Long, OptionMeta> flattenOptions(Map<Long, List<OptionMeta>> byQuestion) {
        Map<Long, OptionMeta> byId = new LinkedHashMap<>();
        for (List<OptionMeta> list : byQuestion.values()) {
            for (OptionMeta o : list) byId.put(o.optionId, o);
        }
        return byId;
    }

    private void questionAndOptionCheck(List<AnswerRecord> answers,
                                        Map<Long, QuestionMeta> questionMeta,
                                        Map<Long, List<OptionMeta>> optionsByQuestion,
                                        Map<Long, OptionMeta> optionsById,
                                        AnswerDataValidationReport report,
                                        boolean enforceTableIdConsistency) {
        for (AnswerRecord a : answers) {
            QuestionMeta q = questionMeta.get(a.getQuestionId());
            if (q == null) {
                report.addError("QUESTION_NOT_FOUND", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        nullableLong(a.getQuestionId()), nullableLong(a.getOptionId()), "question_id", String.valueOf(a.getQuestionId()),
                        "question_id 在 bus_question 中不存在", "修正 question_id，或补充对应问题字典");
                continue;
            }
            if (!"0".equals(nullToZero(q.delFlag))) {
                report.addError("QUESTION_DELETED", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), nullableLong(a.getOptionId()), "question_id", String.valueOf(a.getQuestionId()),
                        "question_id 对应问题已删除或无效", "不要上传已删除问题的数据，或恢复问题字典状态");
            }
            if (a.getTableId() > 0 && q.tableId > 0 && a.getTableId() != q.tableId) {
                addTableIdIssue(report, enforceTableIdConsistency, "QUESTION_TABLE_MISMATCH",
                        a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), nullableLong(a.getOptionId()), String.valueOf(a.getTableId()),
                        "数据行 table_id 与 bus_question.table_id 不一致，问题所属 table_id=" + q.tableId,
                        "按 question_id 所属表修正 table_id，或修正 question_id");
            }
            if ("1".equals(q.bitian) && isBlank(a.getContent()) && a.isActive()) {
                report.addWarn("REQUIRED_CONTENT_EMPTY", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), nullableLong(a.getOptionId()), "content", a.getContent(),
                        "必填题 content 为空: " + safe(q.content), "确认该题是否因逻辑跳转可空；否则补充答案内容");
            }

            List<OptionMeta> optionList = optionsByQuestion.get(a.getQuestionId());
            boolean hasActiveOptions = hasActiveOptions(optionList);
            if (a.getOptionId() <= 0) {
                if (hasActiveOptions && !isBlank(a.getContent()) && !isLikelyFreeTextOrToothPosition(q, a)) {
                    report.addError("OPTION_ID_MISSING", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                            a.getQuestionId(), nullableLong(a.getOptionId()), "option_id", String.valueOf(a.getOptionId()),
                            "该题在 bus_option 中存在有效选项，但数据行 option_id=0/空", "补充正确 option_id；若该题允许 content 存牙位/自由文本，请加入白名单规则");
                }
                continue;
            }

            OptionMeta option = optionsById.get(a.getOptionId());
            if (option == null) {
                report.addError("OPTION_NOT_FOUND", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), a.getOptionId(), "option_id", String.valueOf(a.getOptionId()),
                        "option_id 在 bus_option 中不存在", "修正 option_id 或补充选项字典");
                continue;
            }
            if (option.questionId != a.getQuestionId()) {
                report.addError("OPTION_QUESTION_MISMATCH", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), a.getOptionId(), "option_id", String.valueOf(a.getOptionId()),
                        "option_id 所属 question_id=" + option.questionId + "，与数据行 question_id 不一致",
                        "按 option_id 所属问题修正 question_id，或重新选择正确 option_id");
            }
            if (a.getTableId() > 0 && option.tableId > 0 && option.tableId != a.getTableId()) {
                addTableIdIssue(report, enforceTableIdConsistency, "OPTION_TABLE_MISMATCH",
                        a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), a.getOptionId(), String.valueOf(a.getTableId()),
                        "option_id 所属 table_id=" + option.tableId + "，与数据行 table_id 不一致",
                        "修正 table_id 或 option_id");
            }
            if (!"0".equals(nullToZero(option.delFlag))) {
                report.addError("OPTION_DELETED", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), a.getOptionId(), "option_id", String.valueOf(a.getOptionId()),
                        "option_id 对应选项已删除或无效", "不要上传已删除选项的数据，或恢复选项字典状态");
            }
            if (!isBlank(a.getContent()) && !isBlank(option.code) && !cleanEquals(a.getContent(), option.code)) {
                report.addError("CONTENT_OPTION_CODE_MISMATCH", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), a.getOptionId(), "content", a.getContent(),
                        "content 与 bus_option.code 不一致，选项 code=" + option.code,
                        "若是单选/多选题，应使 content 等于 option.code；若是填空题，不应带 option_id");
            }
        }
    }

    private void duplicateCheck(List<AnswerRecord> answers, Map<Long, QuestionMeta> questionMeta, AnswerDataValidationReport report) {
        Map<String, List<AnswerRecord>> byKey = new LinkedHashMap<>();
        Map<String, List<AnswerRecord>> sameOptionKey = new LinkedHashMap<>();
        for (AnswerRecord a : answers) {
            if (!a.isActive()) continue;
            String base = a.getProjectId() + "|" + a.getTableId() + "|" + a.getYear() + "|" + a.getSampleKey() + "|" + a.getQuestionId();
            addToList(byKey, base, a);
            String withOption = base + "|" + a.getOptionId();
            addToList(sameOptionKey, withOption, a);
        }
        for (List<AnswerRecord> list : sameOptionKey.values()) {
            if (list.size() > 1) {
                AnswerRecord a = list.get(0);
                report.addError("DUPLICATE_SAME_OPTION", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), nullableLong(a.getOptionId()), "question_id/option_id", a.getQuestionId() + "/" + a.getOptionId(),
                        "同一学生同一问题同一选项出现重复有效答案，重复数=" + list.size(), "保留最新或唯一有效答案，其余置 del_flag=2");
            }
        }
        for (List<AnswerRecord> list : byKey.values()) {
            if (list.size() <= 1) continue;
            AnswerRecord a = list.get(0);
            QuestionMeta q = questionMeta.get(a.getQuestionId());
            if (q != null && isMultiChoice(q)) continue;
            Set<Long> optionIds = new LinkedHashSet<>();
            for (AnswerRecord r : list) optionIds.add(r.getOptionId());
            if (optionIds.size() > 1) {
                report.addWarn("DUPLICATE_QUESTION", a.getRowNumber(), a.getSampleKey(), nullableLong(a.getStudentId()),
                        a.getQuestionId(), null, "question_id", String.valueOf(a.getQuestionId()),
                        "同一学生同一非多选题存在多条有效答案，答案数=" + list.size(), "确认是否为多选题；否则仅保留一条有效答案");
            }
        }
    }

    private void addTableIdIssue(AnswerDataValidationReport report,
                                 boolean asError,
                                 String code,
                                 int rowNumber,
                                 String sampleKey,
                                 Long studentId,
                                 Long questionId,
                                 Long optionId,
                                 String actualValue,
                                 String message,
                                 String suggestion) {
        if (asError) {
            report.addError(code, rowNumber, sampleKey, studentId, questionId, optionId,
                    "table_id", actualValue, message, suggestion);
        } else {
            report.addWarn(code, rowNumber, sampleKey, studentId, questionId, optionId,
                    "table_id", actualValue, message, suggestion);
        }
    }

    private void coverageCheck(List<AnswerRecord> answers, Map<Long, QuestionMeta> questionMeta, AnswerDataValidationReport report) {
        if (questionMeta.isEmpty()) return;
        Set<Long> tableIds = new LinkedHashSet<>();
        for (AnswerRecord a : answers) if (a.getTableId() > 0) tableIds.add(a.getTableId());
        if (tableIds.size() != 1) return;
        long tableId = tableIds.iterator().next();

        Set<Long> requiredQuestions = new LinkedHashSet<>();
        for (QuestionMeta q : questionMeta.values()) {
            if (q.tableId == tableId && "1".equals(q.bitian) && "0".equals(nullToZero(q.delFlag))) {
                requiredQuestions.add(q.questionId);
            }
        }
        if (requiredQuestions.isEmpty()) return;

        Map<String, Set<Long>> studentQuestions = new LinkedHashMap<>();
        Map<String, Long> studentIdMap = new LinkedHashMap<>();
        for (AnswerRecord a : answers) {
            if (!a.isActive()) continue;
            Set<Long> set = studentQuestions.get(a.getSampleKey());
            if (set == null) {
                set = new LinkedHashSet<>();
                studentQuestions.put(a.getSampleKey(), set);
                studentIdMap.put(a.getSampleKey(), a.getStudentId());
            }
            set.add(a.getQuestionId());
        }
        int incompleteStudents = 0;
        for (Map.Entry<String, Set<Long>> e : studentQuestions.entrySet()) {
            List<Long> missing = new ArrayList<Long>();
            for (Long qid : requiredQuestions) {
                if (!e.getValue().contains(qid)) missing.add(qid);
                if (missing.size() >= 20) break;
            }
            if (!missing.isEmpty()) {
                incompleteStudents++;
                if (incompleteStudents <= 20) {
                    report.addWarn("MISSING_REQUIRED_QUESTIONS", 0, e.getKey(), nullableLong(studentIdMap.get(e.getKey())),
                            null, null, "question_id", missing.toString(),
                            "该学生缺少必填题记录，最多展示前 20 个 question_id", "确认是否因逻辑跳转可缺失；否则补齐缺失题目记录");
                }
            }
        }
        report.getSummary().put("requiredQuestionCount", requiredQuestions.size());
        report.getSummary().put("incompleteStudentCount", incompleteStudents);
    }

    private boolean hasActiveOptions(List<OptionMeta> options) {
        if (options == null) return false;
        for (OptionMeta o : options) if ("0".equals(nullToZero(o.delFlag))) return true;
        return false;
    }

    private boolean isLikelyFreeTextOrToothPosition(QuestionMeta q, AnswerRecord a) {
        String text = safe(q == null ? null : q.content);
        String type = safe(q == null ? null : q.type);
        if (text.contains("牙位")) return true;
        if (type.contains("填空") || type.contains("文本") || type.contains("输入")) return true;
        // Some tooth position values are comma-separated numbers/letters in content.
        String c = safe(a.getContent());
        return text.contains("龋") && c.matches("^[0-9A-Za-z,，;；、 -]+$");
    }

    private boolean isMultiChoice(QuestionMeta q) {
        String t = safe(q == null ? null : q.type);
        return t.contains("多选");
    }

    private void addToList(Map<String, List<AnswerRecord>> map, String key, AnswerRecord a) {
        List<AnswerRecord> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(a);
    }

    private List<List<Long>> batches(Set<Long> ids, int batchSize) {
        List<List<Long>> batches = new ArrayList<List<Long>>();
        List<Long> cur = new ArrayList<Long>(batchSize);
        for (Long id : ids) {
            cur.add(id);
            if (cur.size() >= batchSize) {
                batches.add(cur);
                cur = new ArrayList<Long>(batchSize);
            }
        }
        if (!cur.isEmpty()) batches.add(cur);
        return batches;
    }

    private String placeholders(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private Long nullableLong(long v) {
        return v <= 0 ? null : v;
    }

    private Long nullableLong(Long v) {
        return v == null || v <= 0 ? null : v;
    }

    private long asLong(Object v) {
        if (v == null) return -1L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v).trim()); } catch (Exception e) { return -1L; }
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String nullToZero(String s) {
        return s == null || s.trim().isEmpty() ? "0" : s.trim();
    }

    private boolean cleanEquals(String a, String b) {
        return normalizeValue(a).equals(normalizeValue(b));
    }

    private String normalizeValue(String s) {
        if (s == null) return "";
        String v = s.replace("\t", "").trim();
        if (v.endsWith(".0")) v = v.substring(0, v.length() - 2);
        return v;
    }

    private static class QuestionMeta {
        long questionId;
        long tableId;
        String content;
        String type;
        String bitian;
        String delFlag;
    }

    private static class OptionMeta {
        long optionId;
        long questionId;
        long tableId;
        String code;
        String content;
        String delFlag;
    }
}
