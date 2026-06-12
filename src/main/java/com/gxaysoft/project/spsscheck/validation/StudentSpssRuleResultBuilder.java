package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule;

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
                                            List<SpssCheckRule> rules,
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
                List<Map<String, Object>> failedRules = new ArrayList<Map<String, Object>>();

                for (RuleView rule : checkRules) {
                    int flag = row.getFlag(rule.target);
                    if (flag == 0) {
                        passedCodes.add(rule.code);
                        passedRuleCount++;
                    } else {
                        String display = rule.code + "丨" + rule.description;
                        failedTexts.add(display);
                        failedRuleCount++;
                        Map<String, Object> detail = new LinkedHashMap<String, Object>();
                        detail.put("ruleCode", rule.code);
                        detail.put("target", rule.target);
                        detail.put("description", rule.description);
                        detail.put("displayText", display);
                        detail.put("value", row.get(rule.target));
                        failedRules.add(detail);
                    }
                }

                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("studentId", info.studentId);
                item.put("studentKey", info.studentKey);
                item.put("studentName", info.studentName);
                item.put("passedRuleCodes", passedCodes);
                item.put("failedRules", failedRules);
                item.put("passedText", joinLines(passedCodes));
                item.put("failedText", joinLines(failedTexts));
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

    private static List<RuleView> checkRules(List<SpssCheckRule> rules) {
        List<RuleView> result = new ArrayList<RuleView>();
        if (rules == null) return result;
        int n = 0;
        for (SpssCheckRule rule : rules) {
            if (rule == null || !rule.isCheckRule()) continue;
            n++;
            result.add(new RuleView(String.format("R%03d", n), rule.getTarget(), description(rule)));
        }
        return result;
    }

    private static String description(SpssCheckRule rule) {
        String label = rule.getLabel();
        if (!isBlank(label)) return label.trim();
        String description = rule.getDescription();
        if (!isBlank(description)) return description.trim();
        return rule.getTarget();
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

        RuleView(String code, String target, String description) {
            this.code = code;
            this.target = target;
            this.description = description;
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
