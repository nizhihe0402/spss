package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentSpssRuleResultBuilderTest {

    @Test
    void buildsStudentRowsWithPassedCodesAndFailedCodeDescriptionsSeparatedByNewline() {
        SpssCheckRule rule1 = new SpssCheckRule("CHECK_A", "", "检查A", Collections.<String>emptyList(),
                true, "", "");
        SpssCheckRule rule2 = new SpssCheckRule("CHECK_B", "", "检查B", Collections.<String>emptyList(),
                true, "", "");
        SpssCheckRule compute = new SpssCheckRule("MID", "A+B", "中间变量", Collections.<String>emptyList(),
                false, "", "");

        RowContext row = new RowContext("1001");
        row.putFlag("CHECK_A", 0);
        row.putFlag("CHECK_B", 1);

        AnswerRecord answer = new AnswerRecord(1, 2, "1001", 10, 0, 1001,
                "x", 1, 1, "", "2025", "0");

        Map<String, Object> result = StudentSpssRuleResultBuilder.build(
                Collections.singletonList(row),
                Arrays.asList(rule1, compute, rule2),
                Collections.singletonList(answer),
                Collections.singletonMap("1001", "张三"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertEquals(1, students.size());
        assertEquals("R001", students.get(0).get("passedText"));
        assertEquals("R002丨检查B", students.get(0).get("failedText"));
        assertEquals(1, result.get("passedRuleCount"));
        assertEquals(1, result.get("failedRuleCount"));
    }
}
