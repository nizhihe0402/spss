package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnswerPivot {
    private AnswerPivot() {
    }

    public static List<RowContext> pivot(List<AnswerRecord> answers, Map<String, QuestionMapping> mappings) {
        Map<Long, String> questionToVariable = new LinkedHashMap<>();
        for (Map.Entry<String, QuestionMapping> entry : mappings.entrySet()) {
            questionToVariable.put(entry.getValue().getQuestionId(), entry.getKey());
        }
        Map<String, RowContext> rows = new LinkedHashMap<>();
        for (AnswerRecord answer : answers) {
            String variable = questionToVariable.get(answer.getQuestionId());
            if (variable == null) {
                continue;
            }
            RowContext row = rows.get(answer.getSampleKey());
            if (row == null) {
                row = new RowContext(answer.getSampleKey());
                rows.put(answer.getSampleKey(), row);
            }
            row.put(variable, answer.getContent());
        }
        return new ArrayList<>(rows.values());
    }
}
