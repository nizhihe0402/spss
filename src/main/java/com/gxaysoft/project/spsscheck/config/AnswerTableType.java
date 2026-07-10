package com.gxaysoft.project.spsscheck.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据表类型 — 替代硬编码的 isTableOne()/isTableTwo()/isTableThree() 方法。
 */
public enum AnswerTableType {
    /** 表1-X：用户调查表 → bus_user_answer */
    USER_ANSWER(new HashSet<>(Arrays.asList(1L, 2L, 10L))),

    /** 表2-X：医生体检表 → bus_doctor_answer */
    DOCTOR_ANSWER(new HashSet<>(Arrays.asList(3L, 4L, 5L))),

    /** 表3-X：学生问卷表 → bus_student_answer */
    STUDENT_ANSWER(new HashSet<>(Arrays.asList(6L, 7L, 8L)));

    private final Set<Long> tableIds;

    AnswerTableType(Set<Long> tableIds) {
        this.tableIds = tableIds;
    }

    public static AnswerTableType fromTableId(long tableId) {
        for (AnswerTableType type : values()) {
            if (type.tableIds.contains(tableId)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的 tableId: " + tableId);
    }

    public boolean hasTimesColumn() {
        return this == DOCTOR_ANSWER;
    }

    public boolean isUserAnswer() {
        return this == USER_ANSWER;
    }

    public String studentIdColumn() {
        return isUserAnswer() ? "code" : "student_id";
    }

    public String answerTableName(String source) {
        if ("intervene".equalsIgnoreCase(source == null ? "" : source.trim())) {
            if (this == DOCTOR_ANSWER) return "bus_doctor_answer_intervene";
            if (this == STUDENT_ANSWER) return "bus_student_answer_intervene";
            throw new IllegalArgumentException("表1-X 暂不支持 source=intervene");
        }
        switch (this) {
            case USER_ANSWER:   return "bus_user_answer";
            case DOCTOR_ANSWER: return "bus_doctor_answer";
            case STUDENT_ANSWER: return "bus_student_answer";
            default: throw new IllegalArgumentException("未知表类型");
        }
    }
}
