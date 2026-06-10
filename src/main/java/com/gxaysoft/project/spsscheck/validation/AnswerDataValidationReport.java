package com.gxaysoft.project.spsscheck.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnswerDataValidationReport {
    private int totalRows;
    private int errorCount;
    private int warnCount;
    private final Map<String, Object> summary = new LinkedHashMap<>();
    private final List<AnswerDataValidationIssue> issues = new ArrayList<>();

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getErrorCount() { return errorCount; }
    public int getWarnCount() { return warnCount; }
    public Map<String, Object> getSummary() { return summary; }
    public List<AnswerDataValidationIssue> getIssues() { return issues; }

    public boolean isPassed() { return errorCount == 0; }

    public void addError(String code, int rowNo, String sampleKey, Long studentId, Long questionId, Long optionId,
                         String fieldName, String fieldValue, String message, String suggestion) {
        errorCount++;
        issues.add(new AnswerDataValidationIssue("ERROR", code, rowNo, sampleKey, studentId, questionId, optionId,
                fieldName, fieldValue, message, suggestion));
    }

    public void addWarn(String code, int rowNo, String sampleKey, Long studentId, Long questionId, Long optionId,
                        String fieldName, String fieldValue, String message, String suggestion) {
        warnCount++;
        issues.add(new AnswerDataValidationIssue("WARN", code, rowNo, sampleKey, studentId, questionId, optionId,
                fieldName, fieldValue, message, suggestion));
    }

    public Map<String, Object> toMap(int issueLimit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("passed", isPassed());
        m.put("totalRows", totalRows);
        m.put("errorCount", errorCount);
        m.put("warnCount", warnCount);
        m.put("summary", summary);
        List<Map<String, Object>> list = new ArrayList<>();
        int count = 0;
        for (AnswerDataValidationIssue issue : issues) {
            if (issueLimit > 0 && count >= issueLimit) break;
            list.add(issue.toMap());
            count++;
        }
        m.put("issues", list);
        m.put("issueLimit", issueLimit);
        m.put("issueTotal", issues.size());
        return m;
    }
}
