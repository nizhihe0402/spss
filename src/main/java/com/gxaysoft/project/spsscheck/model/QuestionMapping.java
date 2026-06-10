package com.gxaysoft.project.spsscheck.model;

public class QuestionMapping {
    private final long questionId;
    private final String variableNameOriginal;
    private final String content;
    private final long tableId;

    public QuestionMapping(long questionId, String variableNameOriginal, String content, long tableId) {
        this.questionId = questionId;
        this.variableNameOriginal = variableNameOriginal;
        this.content = content;
        this.tableId = tableId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public String getVariableNameOriginal() {
        return variableNameOriginal;
    }

    public String getContent() {
        return content;
    }

    public long getTableId() {
        return tableId;
    }
}
