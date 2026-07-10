package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StudentSpssRuleResultBuilder {
    private StudentSpssRuleResultBuilder() {
    }

    public static Map<String, Object> build(List<RowContext> rows,
                                            List<Rule> rules,
                                            List<AnswerRecord> answers,
                                            Map<String, String> studentNamesByKey) {
        Map<String, StudentInfo> studentIndex = indexStudents(answers, studentNamesByKey);
        List<RuleView> checkRules = checkRules(rules);

        List<Map<String, Object>> students = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> passedList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> failedList = new ArrayList<Map<String, Object>>();
        int passedRuleCount = 0;
        int failedRuleCount = 0;

        if (rows != null) {
            for (RowContext row : rows) {
                StudentInfo info = studentIndex.get(row.getSampleKey());
                if (info == null) {
                    info = new StudentInfo(row.getSampleKey(), null, row.getSampleKey(), "");
                }

                List<String> passedCodes = new ArrayList<String>();
                List<String> failedTexts = new ArrayList<String>();
                List<String> failedDetailTexts = new ArrayList<String>();
                List<Map<String, Object>> failedRules = new ArrayList<Map<String, Object>>();

                for (RuleView rule : checkRules) {
                    int flag = row.getFlag(rule.target);
                    if (flag == 0) {
                        passedCodes.add(rule.code);
                        passedRuleCount++;
                    } else {
                        Object value = row.get(rule.target);
                        String display = rule.code + "丨" + rule.description;
                        String detailText = failureDetailText(display, rule, row, value, flag);
                        failedTexts.add(display);
                        failedDetailTexts.add(detailText);
                        failedRuleCount++;
                        Map<String, Object> detail = new LinkedHashMap<String, Object>();
                        detail.put("ruleCode", rule.code);
                        detail.put("target", rule.target);
                        detail.put("description", rule.description);
                        detail.put("displayText", display);
                        detail.put("value", value);
                        detail.put("reason", detailText);
                        failedRules.add(detail);
                    }
                }

                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("studentId", info.studentId != null ? String.valueOf(info.studentId) : null);
                item.put("studentKey", info.studentKey);
                item.put("studentName", info.studentName);
                item.put("passedRuleCodes", passedCodes);
                item.put("failedRules", failedRules);
                item.put("passedText", joinLines(passedCodes));
                item.put("failedText", joinLines(failedTexts));
                item.put("failedDetailText", joinLines(failedDetailTexts));
                item.put("passed", failedRules.isEmpty());
                item.put("failedRuleCount", failedRules.size());
                students.add(item);
                if (failedRules.isEmpty()) {
                    passedList.add(item);
                } else {
                    failedList.add(item);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("studentCount", students.size());
        result.put("ruleCount", checkRules.size());
        result.put("passedStudentCount", passedList.size());
        result.put("failedStudentCount", failedList.size());
        result.put("passedRuleCount", passedRuleCount);
        result.put("failedRuleCount", failedRuleCount);
        result.put("students", students);
        result.put("passedList", passedList);
        result.put("failedList", failedList);
        return result;
    }

    private static List<RuleView> checkRules(List<Rule> rules) {
        List<RuleView> result = new ArrayList<RuleView>();
        if (rules == null) return result;
        int sortNo = 0;
        for (Rule rule : rules) {
            sortNo++;
            if (rule == null) continue;
            // 按 isCheckRule 标志 OR 校验类 RuleType 双重判断
            if (!rule.isCheckRule() && !isCheckType(rule.getType())) continue;
            result.add(new RuleView(String.format("R%03d", sortNo), rule.getTarget(), description(rule),
                    rule.getSourceVariables(), rule.getSpssSource()));
        }
        return result;
    }

    private static boolean isCheckType(RuleType type) {
        if (type == null) return false;
        switch (type) {
            case IDENTITY_CHECK:
            case MISSING_CHECK:
            case CONSISTENCY_CHECK:
            case RANGE_CHECK:
            case DOCUMENT_CHECK:
            case OUTCOME_DETERMINATION:
                return true;
            default:
                return false;
        }
    }

    private static String description(Rule rule) {
        String description = rule.getDescription();
        if (!isBlank(description)) return description.trim();
        return rule.getTarget();
    }

    private static String failureDetailText(String display, RuleView rule, RowContext row, Object value, int flag) {
        StringBuilder sb = new StringBuilder();
        sb.append(display);
        String sourceValues = sourceValuesText(rule.sourceVariables, row);
        if (!isBlank(sourceValues)) {
            sb.append("\n源变量：").append(sourceValues);
        }
        sb.append("\n目标变量：").append(nullToEmpty(rule.target));
        sb.append("\n当前值：").append(displayValue(value));
        sb.append("\n规则结果：").append(flag);
        if (!isBlank(rule.spssSource)) {
            sb.append("\nSPSS规则：").append(oneLine(rule.spssSource));
        }
        sb.append("\n原因：规则结果不为0，按SPSS校验标记为未通过");
        return sb.toString();
    }

    private static String sourceValuesText(List<String> sourceVariables, RowContext row) {
        if (sourceVariables == null || sourceVariables.isEmpty() || row == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String source : sourceVariables) {
            if (isBlank(source)) continue;
            if (sb.length() > 0) sb.append("；");
            sb.append(source.trim()).append("=").append(displayValue(row.get(source)));
        }
        return sb.toString();
    }

    private static String oneLine(String value) {
        if (value == null) return "";
        return value.replace('\r', ' ').replace('\n', ' ').trim().replaceAll("\\s+", " ");
    }

    private static String displayValue(Object value) {
        if (value == null) return "缺失/null";
        String text = String.valueOf(value).trim();
        return text.length() == 0 ? "空值" : text;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, StudentInfo> indexStudents(List<AnswerRecord> answers,
                                                          Map<String, String> studentNamesByKey) {
        Map<String, StudentInfo> result = new LinkedHashMap<String, StudentInfo>();
        if (answers == null) return result;
        Set<String> seen = new LinkedHashSet<String>();
        for (AnswerRecord answer : answers) {
            if (answer == null || isBlank(answer.getSampleKey()) || seen.contains(answer.getSampleKey())) continue;
            seen.add(answer.getSampleKey());
            Long studentId = answer.getStudentId() > 0 ? Long.valueOf(answer.getStudentId()) : null;
            String name = studentNamesByKey == null ? "" : studentNamesByKey.get(answer.getSampleKey());
            result.put(answer.getSampleKey(), new StudentInfo(answer.getSampleKey(), studentId, answer.getSampleKey(), name));
        }
        return result;
    }

    private static String joinLines(Collection<String> values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) return "";
        for (String value : values) {
            if (isBlank(value)) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(value.trim());
        }
        return sb.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static final class RuleView {
        final String code;
        final String target;
        final String description;
        final List<String> sourceVariables;
        final String spssSource;

        RuleView(String code, String target, String description, List<String> sourceVariables, String spssSource) {
            this.code = code;
            this.target = target;
            this.description = description;
            this.sourceVariables = sourceVariables;
            this.spssSource = spssSource;
        }
    }

    private static final class StudentInfo {
        final String key;
        final Long studentId;
        final String studentKey;
        final String studentName;

        StudentInfo(String key, Long studentId, String studentKey, String studentName) {
            this.key = key;
            this.studentId = studentId;
            this.studentKey = studentKey;
            this.studentName = studentName == null ? "" : studentName;
        }
    }
}
