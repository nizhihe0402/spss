package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.RowContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentInfoEnricherTest {

    @Test
    void collectStudentIdsPrefersAnswerStudentIdAndFallsBackToNumericSampleKey() {
        List<AnswerRecord> answers = Arrays.asList(
                new AnswerRecord(-1L, 1, "not-a-student-id", 215002L, -1L,
                        210411106010101L, "129", -1L, 74L, null, null, "0"),
                new AnswerRecord(-1L, 2, "210411106010102", 215003L, -1L,
                        -1L, "27.6", -1L, 74L, null, null, "0"),
                new AnswerRecord(-1L, 3, "code-A", 215004L, -1L,
                        -1L, "1", -1L, 74L, null, null, "0")
        );

        Set<String> studentIds = StudentInfoEnricher.collectStudentIds(answers);

        assertEquals(Arrays.asList("210411106010101", "210411106010102"),
                Arrays.asList(studentIds.toArray(new String[0])));
    }

    @Test
    void enrichRowsDoesNotDirectlyAddCorrectionManagedFields() {
        RowContext row = new RowContext("210411106010101");
        Map<String, String> student = new LinkedHashMap<>();
        student.put("ZJTYPE", "1");
        student.put("SFZ", "511111201701019999");
        student.put("SCHOOL", "06");
        Map<String, Map<String, String>> studentInfo = new LinkedHashMap<>();
        studentInfo.put("210411106010101", student);

        StudentInfoEnricher.enrichRows(Arrays.asList(row), studentInfo);

        assertEquals(null, row.get("ZJTYPE"));
        assertEquals(null, row.get("SFZ"));
        assertEquals(null, row.get("SCHOOL"));
    }

    @Test
    void busStudentIdTypeZeroMapsToSpssIdentityCardType() {
        assertEquals("1", StudentInfoEnricher.toSpssZjtype("0"));
        assertEquals("2", StudentInfoEnricher.toSpssZjtype("2"));
    }

    @Test
    void documentNumberVariableFollowsSpssDocumentType() {
        assertEquals("SFZ", StudentInfoEnricher.documentNumberVariable("1"));
        assertEquals("SFZ", StudentInfoEnricher.documentNumberVariable("0"));
        assertEquals("MTP", StudentInfoEnricher.documentNumberVariable("2"));
        assertEquals("TRPMT", StudentInfoEnricher.documentNumberVariable("3"));
        assertEquals("HZ", StudentInfoEnricher.documentNumberVariable("4"));
    }

    @Test
    void mergeMissingStudentInfoDoesNotOverrideDatabaseRows() {
        Map<String, String> dbStudent = new LinkedHashMap<>();
        dbStudent.put("ZJTYPE", "2");
        Map<String, Map<String, String>> db = new LinkedHashMap<>();
        db.put("210411106010101", dbStudent);

        Map<String, String> fallbackSameStudent = new LinkedHashMap<>();
        fallbackSameStudent.put("ZJTYPE", "1");
        fallbackSameStudent.put("MTP", "M12345678");
        Map<String, String> fallbackMissingStudent = new LinkedHashMap<>();
        fallbackMissingStudent.put("ZJTYPE", "1");
        Map<String, Map<String, String>> fallback = new LinkedHashMap<>();
        fallback.put("210411106010101", fallbackSameStudent);
        fallback.put("210411106010102", fallbackMissingStudent);

        StudentInfoEnricher.mergeMissingStudentInfo(db, fallback);

        assertEquals("2", db.get("210411106010101").get("ZJTYPE"));
        assertEquals("M12345678", db.get("210411106010101").get("MTP"));
        assertEquals("1", db.get("210411106010102").get("ZJTYPE"));
    }
}
