package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.RowContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentSpssRuleResultBuilderTest {

    @Test
    void usesFullRuleListNumbersSoResultCodesMatchRuleListCodes() {
        Rule compute = new Rule("MID", RuleType.COMPUTE_INTERMEDIATE, "", Collections.<String>emptyList());
        compute.setExpression("A+B");
        compute.setDescription("compute");
        Rule checkA = new Rule("CHECK_A", RuleType.IDENTITY_CHECK, "", Collections.<String>emptyList());
        checkA.setCheckRule(true);
        checkA.setDescription("check A");
        Rule checkB = new Rule("CHECK_B", RuleType.IDENTITY_CHECK,
                "RECODE SRC_B (4 thru 6=0) (SYSMIS=1) (ELSE=1) INTO CHECK_B.",
                Collections.singletonList("SRC_B"));
        checkB.setCheckRule(true);
        checkB.setDescription("check B");

        RowContext row = new RowContext("1001");
        row.putFlag("CHECK_A", 0);
        row.putFlag("CHECK_B", 1);
        row.put("CHECK_B", 1);
        row.put("SRC_B", "03");

        AnswerRecord answer = new AnswerRecord(1, 2, "1001", 10, 0, 1001,
                "x", 1, 1, "", "2025", "0");

        Map<String, Object> result = StudentSpssRuleResultBuilder.build(
                Collections.singletonList(row),
                Arrays.asList(compute, checkA, checkB),
                Collections.singletonList(answer),
                Collections.singletonMap("1001", "Student A"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("students");
        assertEquals(1, students.size());
        assertEquals("R002", students.get(0).get("passedText"));
        assertEquals("R003丨check B", students.get(0).get("failedText"));
        assertEquals("R003丨check B\n源变量：SRC_B=03\n目标变量：CHECK_B\n当前值：1\n规则结果：1\nSPSS规则：RECODE SRC_B (4 thru 6=0) (SYSMIS=1) (ELSE=1) INTO CHECK_B.\n原因：规则结果不为0，按SPSS校验标记为未通过",
                students.get(0).get("failedDetailText"));
        assertEquals(1, result.get("passedRuleCount"));
        assertEquals(1, result.get("failedRuleCount"));
    }
}
