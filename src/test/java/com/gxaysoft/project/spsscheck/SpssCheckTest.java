package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.v1.executor.RuleAvailabilityChecker;
import com.gxaysoft.project.spsscheck.v1.executor.RuleEngine;
import com.gxaysoft.project.spsscheck.io.AnswerPivot;
import com.gxaysoft.project.spsscheck.io.OutputWriter;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.io.StudentInfoLoader;
import com.gxaysoft.project.spsscheck.io.TableIdDetector;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.parser.QuestionJsonParser;
import com.gxaysoft.project.spsscheck.parser.QuestionSqlParser;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpssCheckTest {

    private static final Path SQL_PATH = Paths.get("docs/sources/sql/相关表数据.sql");

    static class TableFixture {
        final String name;
        final String spsFile;
        final String csvFile;
        final long tableId;
        final int minRules;
        final int minOutputRules;
        final int minExecutableRules;

        TableFixture(String name, int minRules, int minOutputRules, int minExecutableRules, long tableId) {
            this.name = name;
            this.spsFile = "docs/sources/sps/" + name + ".sps";
            this.csvFile = "docs/sources/cvs/" + name + "/bus_" +
                    (name.startsWith("表1") ? "user" : name.startsWith("表2") ? "doctor" : "student") +
                    "_answer_*.csv";
            this.tableId = tableId;
            this.minRules = minRules;
            this.minOutputRules = minOutputRules;
            this.minExecutableRules = minExecutableRules;
        }
    }

    private static Path resolveCsv(TableFixture fixture) throws Exception {
        java.nio.file.Path dir = Paths.get("docs/sources/cvs/" + fixture.name);
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().startsWith("bus_")
                            && p.getFileName().toString().endsWith(".csv")
                            && !p.getFileName().toString().contains("intervene"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No CSV found for " + fixture.name));
        }
    }

    // ── 表1-x: administrative survey tables ──────────────────────

    @Test
    @DisplayName("表1-1: parse, pivot, execute all rules")
    void testTable11() throws Exception {
        runAndVerify(new TableFixture("表1-1", 10, 5, 13, 1));
    }

    @Test
    @DisplayName("表1-2: parse, pivot, execute all rules")
    void testTable12() throws Exception {
        runAndVerify(new TableFixture("表1-2", 8, 4, 11, 2));
    }

    @Test
    @DisplayName("表1-3: parse, pivot, execute all rules (DO IF blocks)")
    void testTable13() throws Exception {
        runAndVerify(new TableFixture("表1-3", 25, 4, 30, 10));
    }

    // ── 表2-x: student health examination tables ─────────────────

    @Test
    @DisplayName("表2-1: partial executability (unsupported SPSS functions)")
    void testTable21() throws Exception {
        runAndVerify(new TableFixture("表2-1", 40, 12, 38, 3));
    }

    @Test
    @DisplayName("表2-2: partial executability (JSON fallback)")
    void testTable22() throws Exception {
        runAndVerify(new TableFixture("表2-2", 20, 8, 15, 4));
    }

    @Test
    @DisplayName("表2-3: partial executability (JSON fallback)")
    void testTable23() throws Exception {
        runAndVerify(new TableFixture("表2-3", 8, 3, 8, 5));
    }

    // ── 表3-x: student questionnaire tables ──────────────────────

    @Test
    @DisplayName("表3-1: all rules executable (JSON fallback)")
    void testTable31() throws Exception {
        runAndVerify(new TableFixture("表3-1", 8, 3, 10, 6));
    }

    @Test
    @DisplayName("表3-2: all rules executable (JSON fallback)")
    void testTable32() throws Exception {
        runAndVerify(new TableFixture("表3-2", 8, 3, 10, 7));
    }

    @Test
    @DisplayName("表3-3: all rules executable (JSON fallback)")
    void testTable33() throws Exception {
        runAndVerify(new TableFixture("表3-3", 8, 3, 10, 8));
    }

    // ── helper ───────────────────────────────────────────────────

    private void runAndVerify(TableFixture fixture) throws Exception {
        Path csvPath = resolveCsv(fixture);
        Path spsPath = Paths.get(fixture.spsFile);

        String spsText = PrototypeFileReaders.readSpssText(spsPath);
        String sqlText = PrototypeFileReaders.readSpssText(SQL_PATH);
        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvPath);

        long tableId = TableIdDetector.detectMostFrequentTableId(answers);
        if (fixture.tableId > 0) {
            tableId = fixture.tableId;
        }

        Map<String, QuestionMapping> mappings = QuestionSqlParser.parseQuestionMappings(sqlText, tableId);

        // Fallback: if SQL parsing yields no mappings, try the complete JSON export
        if (mappings.isEmpty()) {
            Path jsonPath = Paths.get("docs/sources/sql/bus_question_202606081429.json");
            if (java.nio.file.Files.exists(jsonPath)) {
                mappings = QuestionJsonParser.parseQuestionMappings(jsonPath, tableId);
                System.out.printf("[%s] (using JSON fallback) ", fixture.name);
            }
        }

        // Merge supplementary student info (ZJTYPE, SFZ) if available
        StudentInfoLoader.LoadResult studentData = null;
        Path studentInfoPath = Paths.get("docs/sources/data/学生证件类型证件号.json");
        if (java.nio.file.Files.exists(studentInfoPath)) {
            studentData = StudentInfoLoader.load(studentInfoPath);
            mappings.putAll(studentData.mappings);
        }
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(spsText);
        List<SpssDatasetRule> datasetRules = SpssRuleParser.parseDatasetRules(spsText);
        List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);
        List<RuleAvailability> availability = RuleAvailabilityChecker.check(rules, datasetRules, mappings);

        List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
        if (studentData != null) {
            StudentInfoLoader.enrichRows(rows, studentData.studentInfo);
        }
        RuleEngine.execute(rows, rules);
        for (SpssDatasetRule datasetRule : datasetRules) {
            datasetRule.execute(rows);
        }

        long executable = availability.stream().filter(RuleAvailability::isExecutable).count();

        System.out.printf("[%s] answers=%d mappings=%d rows=%d rules=%d outputRules=%d executable=%d/%d%n",
                fixture.name, answers.size(), mappings.size(), rows.size(),
                rules.size(), outputRules.size(), executable, rules.size());

        // Structural assertions — pipeline must complete without exception
        assertTrue(rules.size() >= fixture.minRules,
                fixture.name + ": expected >= " + fixture.minRules + " rules, got " + rules.size());
        assertTrue(outputRules.size() >= fixture.minOutputRules,
                fixture.name + ": expected >= " + fixture.minOutputRules + " outputRules, got " + outputRules.size());
        assertTrue(executable >= fixture.minExecutableRules,
                fixture.name + ": expected >= " + fixture.minExecutableRules + " executable, got " + executable);
        assertNotNull(rows, fixture.name + ": rows must not be null");
        assertFalse(spsText.isEmpty(), fixture.name + ": SPS text must not be empty");

        // Verify output rules are well-formed
        for (SpssOutputRule or : outputRules) {
            assertNotNull(or.getSheetName(), "output sheet name must not be null");
            assertFalse(or.getSheetName().isEmpty(), "output sheet name must not be empty");
        }

        // Verify all rule steps have source variables
        for (SpssCheckRule rule : rules) {
            assertNotNull(rule.getSourceVariables(), rule.getTarget() + ": sourceVariables must not be null");
        }

        // If no mappings, rows should be 0 (graceful degradation)
        if (mappings.isEmpty()) {
            assertEquals(0, rows.size(), fixture.name + ": 0 mappings → 0 rows expected");
        }

        // Generate Excel/CSV output
        Path outputDir = Paths.get("target/test-output/" + fixture.name);
        if (!rows.isEmpty() && !outputRules.isEmpty()) {
            OutputWriter.writeOutputs(outputDir, rows, outputRules);
            assertTrue(java.nio.file.Files.exists(outputDir), "output dir must exist after write");
        }
    }
}
