package com.gxaysoft.project.spsscheck.execution;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbRuleExecutionDataLoaderTest {

    @Test
    void selectsNormalAndInterveneAnswerTablesByTableGroup() {
        assertEquals("bus_user_answer",
                DbRuleExecutionDataLoader.resolveAnswerTable(1L, "normal"));
        assertEquals("bus_doctor_answer",
                DbRuleExecutionDataLoader.resolveAnswerTable(3L, "normal"));
        assertEquals("bus_student_answer",
                DbRuleExecutionDataLoader.resolveAnswerTable(6L, "normal"));

        assertEquals("bus_doctor_answer_intervene",
                DbRuleExecutionDataLoader.resolveAnswerTable(4L, "intervene"));
        assertEquals("bus_student_answer_intervene",
                DbRuleExecutionDataLoader.resolveAnswerTable(7L, "intervene"));
    }

    @Test
    void rejectsInterveneForTableOneBecauseNoUserInterveneTableExists() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DbRuleExecutionDataLoader.resolveAnswerTable(10L, "intervene"));

        assertEquals("表1-X 暂不支持 source=intervene", ex.getMessage());
    }

    @Test
    void tableTwoSqlSelectsTimesButTableThreeSqlUsesNullTimes() {
        String tableTwoSql = DbRuleExecutionDataLoader.buildSqlForTable(3L, "bus_doctor_answer");
        String tableThreeSql = DbRuleExecutionDataLoader.buildSqlForTable(6L, "bus_student_answer");

        assertTrue(tableTwoSql.contains("a.times"));
        assertFalse(tableThreeSql.contains("a.times"));
        assertTrue(tableThreeSql.contains("NULL AS times"));
    }

    @Test
    void convertsDoctorOrStudentAnswerRowToAnswerRecord() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", 100L);
        row.put("question_id", 220014L);
        row.put("option_id", 0L);
        row.put("student_id", 210911107430506L);
        row.put("content", "13/07/2024");
        row.put("project_id", 2L);
        row.put("table_id", 4L);
        row.put("times", null);
        row.put("year", "2025");
        row.put("del_flag", "0");

        AnswerRecord record = DbRuleExecutionDataLoader.toAnswerRecord(row, false, 3);

        assertEquals(100L, record.getRawId());
        assertEquals(3, record.getRowNumber());
        assertEquals("210911107430506", record.getSampleKey());
        assertEquals(210911107430506L, record.getStudentId());
        assertEquals(220014L, record.getQuestionId());
        assertEquals("13/07/2024", record.getContent());
        assertEquals(2L, record.getProjectId());
        assertEquals(4L, record.getTableId());
        assertEquals("2025", record.getYear());
    }

    @Test
    void convertsUserAnswerCodeToSampleKeyAndStudentId() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", 200L);
        row.put("question_id", 110001L);
        row.put("option_id", 0L);
        row.put("code", 11001001L);
        row.put("content", "1");
        row.put("project_id", 2L);
        row.put("table_id", 1L);
        row.put("year", "2025");
        row.put("del_flag", "0");

        AnswerRecord record = DbRuleExecutionDataLoader.toAnswerRecord(row, true, 9);

        assertEquals("11001001", record.getSampleKey());
        assertEquals(11001001L, record.getStudentId());
        assertEquals(110001L, record.getQuestionId());
        assertEquals("1", record.getContent());
    }
}
