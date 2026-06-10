package com.gxaysoft.project.spsscheck.validation;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnswerDataValidationIssue {
    private final String level;
    private final String code;
    private final int rowNo;
    private final String sampleKey;
    private final Long studentId;
    private final Long questionId;
    private final Long optionId;
    private final String fieldName;
    private final String fieldValue;
    private final String message;
    private final String suggestion;

    public AnswerDataValidationIssue(String level,
                                     String code,
                                     int rowNo,
                                     String sampleKey,
                                     Long studentId,
                                     Long questionId,
                                     Long optionId,
                                     String fieldName,
                                     String fieldValue,
                                     String message,
                                     String suggestion) {
        this.level = level;
        this.code = code;
        this.rowNo = rowNo;
        this.sampleKey = sampleKey;
        this.studentId = studentId;
        this.questionId = questionId;
        this.optionId = optionId;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.message = message;
        this.suggestion = suggestion;
    }

    public String getLevel() { return level; }
    public String getCode() { return code; }
    public int getRowNo() { return rowNo; }
    public String getSampleKey() { return sampleKey; }
    public Long getStudentId() { return studentId; }
    public Long getQuestionId() { return questionId; }
    public Long getOptionId() { return optionId; }
    public String getFieldName() { return fieldName; }
    public String getFieldValue() { return fieldValue; }
    public String getMessage() { return message; }
    public String getSuggestion() { return suggestion; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", level);
        m.put("code", code);
        m.put("rowNo", rowNo);
        m.put("sampleKey", sampleKey);
        m.put("studentId", studentId);
        m.put("questionId", questionId);
        m.put("optionId", optionId);
        m.put("fieldName", fieldName);
        m.put("fieldValue", fieldValue);
        m.put("message", message);
        m.put("suggestion", suggestion);
        return m;
    }
}
