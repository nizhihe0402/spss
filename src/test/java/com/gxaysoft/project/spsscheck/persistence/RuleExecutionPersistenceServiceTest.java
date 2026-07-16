package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleExecutionPersistenceServiceTest {

    @Test
    void derivesCleanAndFailTableNamesFromAnswerTable() {
        assertEquals("sps_bus_doctor_answer_clean",
                RuleExecutionPersistenceService.cleanTableName("bus_doctor_answer"));
        assertEquals("sps_bus_doctor_answer_fail",
                RuleExecutionPersistenceService.failTableName("bus_doctor_answer"));
        assertEquals("sps_bus_doctor_answer_intervene_clean",
                RuleExecutionPersistenceService.cleanTableName("bus_doctor_answer_intervene"));
        assertEquals("sps_bus_student_answer_intervene_clean",
                RuleExecutionPersistenceService.cleanTableName("bus_student_answer_intervene"));
        assertEquals("sps_bus_student_answer_intervene_fail",
                RuleExecutionPersistenceService.failTableName("bus_student_answer_intervene"));
    }

    @Test
    void selectsOnlyPassedStudentSourceRowsForClean() {
        PrototypeFileReaders.AnswerCsvLoadResult load = new PrototypeFileReaders.AnswerCsvLoadResult();
        load.getAnswers().add(answer(11L, "2101", 220001L));
        load.getAnswers().add(answer(12L, "2101", 220002L));
        load.getAnswers().add(answer(21L, "2102", 220001L));

        Map<String, Object> result = resultWithPassedAndFailedStudents();

        List<Long> ids = RuleExecutionPersistenceService.cleanSourceIds(load, result);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(11L));
        assertTrue(ids.contains(12L));
        assertFalse(ids.contains(21L));
    }

    @Test
    void collectsAllLoadedSourceRowsForCleanup() {
        PrototypeFileReaders.AnswerCsvLoadResult load = new PrototypeFileReaders.AnswerCsvLoadResult();
        load.getAnswers().add(answer(11L, "2101", 220001L));
        load.getAnswers().add(answer(21L, "2102", 220001L));

        List<Long> ids = RuleExecutionPersistenceService.allSourceIds(load);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(11L));
        assertTrue(ids.contains(21L));
    }

    @Test
    void buildsFailDetailsFromFailedRulesOnly() {
        Map<String, Object> result = resultWithPassedAndFailedStudents();

        List<RuleExecutionPersistenceService.FailDetail> details =
                RuleExecutionPersistenceService.failDetails(result);

        assertEquals(1, details.size());
        assertEquals("2102", details.get(0).studentKey);
        assertEquals("R024", details.get(0).ruleCode);
        assertEquals("ZJTYPE", details.get(0).ruleTarget);
        assertEquals("证件类型缺失或异常", details.get(0).ruleName);
    }

    private AnswerRecord answer(long rawId, String sampleKey, long questionId) {
        return new AnswerRecord(rawId, 1, sampleKey, questionId, 0L,
                Long.parseLong(sampleKey), "1", 2L, 6L, null, "2025", "0");
    }

    private Map<String, Object> resultWithPassedAndFailedStudents() {
        Map<String, Object> passed = new LinkedHashMap<String, Object>();
        passed.put("studentKey", "2101");
        passed.put("passed", Boolean.TRUE);

        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("ruleCode", "R024");
        rule.put("target", "ZJTYPE");
        rule.put("description", "证件类型缺失或异常");
        rule.put("displayText", "R024丨证件类型缺失或异常");
        rule.put("value", "");
        rule.put("reason", "证件类型为空");

        Map<String, Object> failed = new LinkedHashMap<String, Object>();
        failed.put("studentKey", "2102");
        failed.put("studentId", 2102L);
        failed.put("failedRules", java.util.Collections.singletonList(rule));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("passedList", java.util.Collections.singletonList(passed));
        result.put("failedList", java.util.Collections.singletonList(failed));
        return result;
    }
}
