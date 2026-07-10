package com.gxaysoft.project.spsscheck.execution;

import com.gxaysoft.project.spsscheck.model.RowContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleCorrectionRuntimeServiceTest {

    @Test
    void normalizesRegionCodesToRightTwoDigitsForTableTwoAndThree() {
        RowContext row = new RowContext("210411106010101");
        row.put("PROVINCE", "21");
        row.put("CITY", "2104");
        row.put("COUNTY", "210411");

        RuleCorrectionRuntimeService.CorrectionResult result =
                RuleCorrectionRuntimeService.applyInMemory(Arrays.asList(row),
                        Arrays.asList(plan("NORMALIZE_REGION_CODE", "PROVINCE,CITY,COUNTY")), 3L, null);

        assertEquals("21", row.get("PROVINCE"));
        assertEquals("04", row.get("CITY"));
        assertEquals("11", row.get("COUNTY"));
        assertEquals("04", result.valueFor("210411106010101", "CITY"));
        assertEquals("11", result.valueFor("210411106010101", "COUNTY"));
    }

    @Test
    void fillsMissingSchoolCodeFromStudentInfo() {
        RowContext row = new RowContext("210411106010101");
        row.put("SCHOOL", "");
        Map<String, String> student = new LinkedHashMap<String, String>();
        student.put("SCHOOL", "06");
        Map<String, Map<String, String>> studentInfo = new LinkedHashMap<String, Map<String, String>>();
        studentInfo.put("210411106010101", student);

        RuleCorrectionRuntimeService.CorrectionResult result =
                RuleCorrectionRuntimeService.applyInMemory(Arrays.asList(row),
                        Arrays.asList(plan("FILL_SCHOOL_CODE", "SCHOOL")), 3L, studentInfo);

        assertEquals("06", row.get("SCHOOL"));
        assertEquals("06", result.valueFor("210411106010101", "SCHOOL"));
    }

    @Test
    void fillsMissingDocumentInfoFromStudentInfo() {
        RowContext row = new RowContext("210411106010101");
        row.put("ZJTYPE", "");
        row.put("SFZ", "");
        Map<String, String> student = new LinkedHashMap<String, String>();
        student.put("ZJTYPE", "1");
        student.put("SFZ", "210411201701019999");
        Map<String, Map<String, String>> studentInfo = new LinkedHashMap<String, Map<String, String>>();
        studentInfo.put("210411106010101", student);

        RuleCorrectionRuntimeService.CorrectionResult result =
                RuleCorrectionRuntimeService.applyInMemory(Arrays.asList(row),
                        Arrays.asList(plan("FILL_DOCUMENT_INFO", "ZJTYPE,SFZ")), 3L, studentInfo);

        assertEquals("1", row.get("ZJTYPE"));
        assertEquals("210411201701019999", row.get("SFZ"));
        assertEquals("1", result.valueFor("210411106010101", "ZJTYPE"));
        assertEquals("210411201701019999", result.valueFor("210411106010101", "SFZ"));
    }

    @Test
    void documentCorrectionDoesNotOverrideExistingValues() {
        RowContext row = new RowContext("210411106010101");
        row.put("ZJTYPE", "2");
        Map<String, String> student = new LinkedHashMap<String, String>();
        student.put("ZJTYPE", "1");
        Map<String, Map<String, String>> studentInfo = new LinkedHashMap<String, Map<String, String>>();
        studentInfo.put("210411106010101", student);

        RuleCorrectionRuntimeService.CorrectionResult result =
                RuleCorrectionRuntimeService.applyInMemory(Arrays.asList(row),
                        Arrays.asList(plan("FILL_DOCUMENT_INFO", "ZJTYPE")), 3L, studentInfo);

        assertEquals("2", row.get("ZJTYPE"));
        assertEquals(null, result.valueFor("210411106010101", "ZJTYPE"));
    }

    @Test
    void fillsMissingNonIdentityDocumentNumbersFromStudentInfo() {
        RowContext row = new RowContext("210411106010101");
        row.put("MTP", "");
        row.put("TRPMT", "");
        row.put("HZ", "");
        Map<String, String> student = new LinkedHashMap<String, String>();
        student.put("MTP", "M12345678");
        student.put("TRPMT", "T1234567");
        student.put("HZ", "H12345678");
        Map<String, Map<String, String>> studentInfo = new LinkedHashMap<String, Map<String, String>>();
        studentInfo.put("210411106010101", student);

        RuleCorrectionRuntimeService.CorrectionResult result =
                RuleCorrectionRuntimeService.applyInMemory(Arrays.asList(row),
                        Arrays.asList(plan("FILL_DOCUMENT_INFO", "MTP,TRPMT,HZ")), 3L, studentInfo);

        assertEquals("M12345678", row.get("MTP"));
        assertEquals("T1234567", row.get("TRPMT"));
        assertEquals("H12345678", row.get("HZ"));
        assertEquals("M12345678", result.valueFor("210411106010101", "MTP"));
        assertEquals("T1234567", result.valueFor("210411106010101", "TRPMT"));
        assertEquals("H12345678", result.valueFor("210411106010101", "HZ"));
    }

    private RuleCorrectionRuntimeService.CorrectionPlan plan(String type, String variables) {
        return new RuleCorrectionRuntimeService.CorrectionPlan(true, type, variables, true, false);
    }
}
