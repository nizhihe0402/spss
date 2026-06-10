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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simulates the full lifecycle: upload .sps → parse → validate → persistence → execute → report.
 * Writes all results as Excel-readable CSV files under {@code output/sps-run/<timestamp>/}.
 *
 * Usage: java ... SpsUploadSimulator [sps-dir] [sql-or-json] [csv-data-dir]
 */
public class SpsUploadSimulator {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public static void main(String[] args) throws Exception {
        Path spsDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");
        Path mappingFile = args.length >= 2 ? Paths.get(args[1]) : Paths.get("docs/sources/sql/bus_question_202606081429.json");
        Path csvDataDir = args.length >= 3 ? Paths.get(args[2]) : Paths.get("docs/sources/cvs");

        Path runDir = Paths.get("output/sps-run/" + TS.format(new Date()));
        Files.createDirectories(runDir);

        // Load mapping source once (JSON or SQL)
        String mappingSource = mappingFile.toString().endsWith(".json")
                ? "JSON: " + mappingFile.getFileName()
                : "SQL: " + mappingFile.getFileName();

        System.out.println("=== SPS Upload Simulator ===");
        System.out.println("SPS dir    : " + spsDir.toAbsolutePath());
        System.out.println("Mapping    : " + mappingSource);
        System.out.println("CSV data   : " + csvDataDir.toAbsolutePath());
        System.out.println("Output     : " + runDir.toAbsolutePath());
        System.out.println();

        // 1. Discover SPS files
        List<Path> spsFiles = Files.list(spsDir)
                .filter(p -> p.toString().endsWith(".sps"))
                .sorted()
                .collect(Collectors.toList());

        if (spsFiles.isEmpty()) {
            System.out.println("No .sps files found in " + spsDir);
            return;
        }

        System.out.println("Found " + spsFiles.size() + " SPS files");
        System.out.println();

        List<SpsRunResult> results = new ArrayList<>();

        for (Path spsFile : spsFiles) {
            String spsName = spsFile.getFileName().toString().replace(".sps", "");
            System.out.println("────────────────────────────────────────────");
            System.out.println("Processing: " + spsName);

            SpsRunResult result = processSps(spsFile, mappingFile, csvDataDir, runDir, spsName);
            results.add(result);
            System.out.println();
        }

        // 2. Write summary report
        writeSummaryReport(runDir, results);
        System.out.println("=== Done. Output: " + runDir.toAbsolutePath() + " ===");
    }

    static SpsRunResult processSps(Path spsFile, Path mappingFile, Path csvDataDir,
                                   Path runDir, String spsName) throws Exception {
        SpsRunResult result = new SpsRunResult();
        result.spsName = spsName;

        // ── Parse SPS ──
        String spsText = PrototypeFileReaders.readSpssText(spsFile);
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(spsText);
        List<SpssDatasetRule> datasetRules = SpssRuleParser.parseDatasetRules(spsText);
        List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);

        result.totalRules = rules.size();
        result.totalDatasetRules = datasetRules.size();
        result.totalOutputRules = outputRules.size();
        result.spsSize = spsText.length();

        System.out.printf("  Parse: %d rules, %d dataset rules, %d output rules (%d chars)%n",
                rules.size(), datasetRules.size(), outputRules.size(), spsText.length());

        // ── Collect unsupported statements ──
        collectUnsupportedStatements(spsText, result);

        // ── Write parse report (simulates sps_rule + sps_rule_step tables) ──
        writeParseReport(runDir, spsName, rules, datasetRules, outputRules, result);

        // ── Find matching CSV data ──
        Path csvFile = findCsvForTable(csvDataDir, spsName);
        if (csvFile == null) {
            System.out.println("  No CSV data found — skipping execution");
            result.skipped = true;
            return result;
        }

        // ── Load mappings ──
        Map<String, QuestionMapping> mappings = loadMappings(mappingFile, csvFile);

        // ── Load supplementary student info (ZJTYPE, SFZ) ──
        Path studentInfoPath = Paths.get("docs/sources/data/学生证件类型证件号.json");
        StudentInfoLoader.LoadResult studentData = null;
        if (Files.exists(studentInfoPath)) {
            studentData = StudentInfoLoader.load(studentInfoPath);
            mappings.putAll(studentData.mappings);
            System.out.printf("  Student info: %d records loaded%n", studentData.studentInfo.size());
        }
        result.totalMappings = mappings.size();

        // ── Load answers & pivot ──
        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvFile);
        long tableId = TableIdDetector.detectMostFrequentTableId(answers);
        result.tableId = tableId;
        result.totalAnswers = answers.size();

        List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
        if (studentData != null) {
            StudentInfoLoader.enrichRows(rows, studentData.studentInfo);
        }
        result.totalRows = rows.size();

        System.out.printf("  Data: tableId=%d, %d answers, %d mappings, %d rows%n",
                tableId, answers.size(), mappings.size(), rows.size());

        if (rows.isEmpty()) {
            System.out.println("  No rows pivoted — skipping execution");
            result.skipped = true;
            return result;
        }

        // ── Execute rules ──
        RuleEngine.execute(rows, rules);
        for (SpssDatasetRule dr : datasetRules) {
            dr.execute(rows);
        }

        // ── Availability check ──
        List<RuleAvailability> availability = RuleAvailabilityChecker.check(rules, datasetRules, mappings);
        result.executableRules = availability.stream().filter(RuleAvailability::isExecutable).count();
        result.unexecutableRules = result.totalRules - result.executableRules;

        for (RuleAvailability a : availability) {
            if (!a.isExecutable()) {
                result.failedRules.add(a.getRule().getTarget() + " missing=" + a.getMissingVariables());
            }
        }

        System.out.printf("  Execute: %d/%d rules executable%n", result.executableRules, result.totalRules);

        // ── Write execution report (simulates sps_check_error_detail table) ──
        writeExecutionReport(runDir, spsName, outputRules, rows, result);

        // ── Write output groups (simulates SAVE OUTFILE → Excel) ──
        Path outputDir = runDir.resolve(spsName + "-outputs");
        OutputWriter.writeOutputs(outputDir, rows, outputRules);

        return result;
    }

    private static Map<String, QuestionMapping> loadMappings(Path mappingFile, Path csvFile) throws Exception {
        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvFile);
        long tableId = TableIdDetector.detectMostFrequentTableId(answers);

        if (mappingFile.toString().endsWith(".json")) {
            return QuestionJsonParser.parseQuestionMappings(mappingFile, tableId);
        } else {
            String sqlText = PrototypeFileReaders.readSpssText(mappingFile);
            return QuestionSqlParser.parseQuestionMappings(sqlText, tableId);
        }
    }

    private static Path findCsvForTable(Path csvDataDir, String spsName) throws IOException {
        Path tableDir = csvDataDir.resolve(spsName);
        if (!Files.isDirectory(tableDir)) {
            // Try with 表 prefix
            return null;
        }
        return Files.list(tableDir)
                .filter(p -> p.getFileName().toString().endsWith(".csv")
                        && !p.getFileName().toString().contains("intervene"))
                .findFirst()
                .orElse(null);
    }

    private static void collectUnsupportedStatements(String spsText, SpsRunResult result) {
        String[] unsupported = {
                "DESCRIPTIVES", "FREQUENCIES", "CTABLES", "DATASET COPY", "DATASET ACTIVATE",
                "FILTER OFF", "USE ALL", "LEAVE", "FORMAT", "VARIABLE LEVEL", "VARIABLE WIDTH",
                "SPLIT FILE", "EXECUTE", "DATEDIFF", "DATEDIF", "MISSING(", "$SYSMIS",
                "XDATE.YEAR", "XDATE.MONTH", "XDATE.MDAY", "NUMBER(", "CHAR.SUBSTR",
                "CHAR.LENGTH", "STRING(", "MOD(", "RND(", "LTRIM(", "RTRIM("
        };
        for (String kw : unsupported) {
            if (spsText.toUpperCase(Locale.ROOT).contains(kw.toUpperCase(Locale.ROOT))) {
                result.unsupportedStatements.add(kw);
            }
        }
        System.out.printf("  Unsupported: %d types detected%n", result.unsupportedStatements.size());
    }

    // ── Parse report: simulates sps_script + sps_rule + sps_rule_step tables ──

    private static void writeParseReport(Path runDir, String spsName,
                                         List<SpssCheckRule> rules,
                                         List<SpssDatasetRule> datasetRules,
                                         List<SpssOutputRule> outputRules,
                                         SpsRunResult result) throws IOException {
        Path reportDir = runDir.resolve(spsName + "-parse");
        Files.createDirectories(reportDir);

        // sps_script (one row per SPS file)
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                reportDir.resolve("sps_script.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("script_name,table_code,parse_status,total_rules,dataset_rules,output_rules,unsupported_count,sps_size");
            w.printf("%s,,SUCCESS,%d,%d,%d,%d,%d%n",
                    spsName, rules.size(), datasetRules.size(), outputRules.size(),
                    result.unsupportedStatements.size(), result.spsSize);
        }

        // sps_rule (one row per check rule)
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                reportDir.resolve("sps_rule.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("rule_code,target_variable,rule_type,source_variables,step_count,label,spss_source_size,java_preview");
            int idx = 0;
            for (SpssCheckRule rule : rules) {
                String code = spsName + "-R" + String.format("%03d", ++idx);
                w.printf("%s,%s,%s,\"%s\",%d,\"%s\",%d,\"%s\"%n",
                        code, rule.getTarget(),
                        rule.isCheckRule() ? "ROW_CHECK" : "COMPUTE",
                        String.join(";", rule.getSourceVariables()),
                        rule.getSteps().size(),
                        rule.getLabel() != null ? rule.getLabel().replace("\"", "\"\"") : "",
                        rule.getSpssSource().length(),
                        rule.getJavaRule().replace("\"", "\"\""));
            }
        }

        // sps_rule_step (one row per step)
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                reportDir.resolve("sps_rule_step.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("rule_code,step_no,step_type,source,target,condition,expression");
            int ruleIdx = 0;
            for (SpssCheckRule rule : rules) {
                ruleIdx++;
                String code = spsName + "-R" + String.format("%03d", ruleIdx);
                int stepNo = 0;
                for (RuleStep step : rule.getSteps()) {
                    stepNo++;
                    String type = step.getClass().getSimpleName().replace("RuleStep", "").toUpperCase();
                    String source = "", target = "", condition = "", expression = "";
                    if (step instanceof ComputeRuleStep) {
                        ComputeRuleStep cs = (ComputeRuleStep) step;
                        source = ""; target = cs.getTarget(); expression = cs.getExpression();
                    } else if (step instanceof RecodeRuleStep) {
                        RecodeRuleStep rs = (RecodeRuleStep) step;
                        source = rs.sourceVariables().get(0); target = "";
                    } else if (step instanceof IfAssignRuleStep) {
                        IfAssignRuleStep is = (IfAssignRuleStep) step;
                        condition = ""; target = ""; expression = "";
                    } else if (step instanceof ConditionalRuleStep) {
                        ConditionalRuleStep cs = (ConditionalRuleStep) step;
                        condition = ""; // extract from javaRule
                    }
                    w.printf("%s,%d,%s,%s,%s,\"%s\",\"%s\"%n",
                            code, stepNo, type, source, target,
                            condition.replace("\"", "\"\""), expression.replace("\"", "\"\""));
                }
            }
        }

        // sps_output_rule
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                reportDir.resolve("sps_output_rule.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("output_code,output_name,output_type,select_condition,spss_source_size");
            int idx = 0;
            for (SpssOutputRule or : outputRules) {
                String code = spsName + "-O" + String.format("%03d", ++idx);
                String type = or.getSheetName().contains("清理后") ? "CLEAN_DATA" : "ERROR_GROUP";
                w.printf("%s,\"%s\",%s,\"%s\",%d%n",
                        code, or.getSheetName(), type,
                        or.getCondition().replace("\"", "\"\""),
                        or.getSpssSource().length());
            }
        }

        // sps_unsupported_statement
        if (!result.unsupportedStatements.isEmpty()) {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                    reportDir.resolve("sps_unsupported_statement.csv"), StandardCharsets.UTF_8))) {
                w.print('﻿');
                w.println("statement_type,reason,risk_level");
                for (String stmt : result.unsupportedStatements) {
                    String risk = stmt.contains("MISSING") || stmt.contains("SYSMIS") ? "HIGH" : "MEDIUM";
                    w.printf("%s,SPSS function not yet supported by engine,%s%n", stmt, risk);
                }
            }
        }

        System.out.printf("  Parse report: %s-parse/%n", spsName);
    }

    // ── Execution report: simulates sps_check_error_detail + run batch ──

    private static void writeExecutionReport(Path runDir, String spsName,
                                             List<SpssOutputRule> outputRules,
                                             List<RowContext> rows,
                                             SpsRunResult result) throws IOException {
        Path reportDir = runDir.resolve(spsName + "-parse");
        Files.createDirectories(reportDir);

        // sps_run_batch
        long totalErrors = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                reportDir.resolve("sps_run_batch.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("table_code,table_id,total_rows,clean_rows,error_rows,executable_rules,total_rules,status");
            for (SpssOutputRule or : outputRules) {
                int count = 0;
                for (RowContext row : rows) {
                    if (or.matches(row)) count++;
                }
                if (!or.getSheetName().contains("清理后")) {
                    totalErrors += count;
                }
            }
            long cleanRows = rows.size() - totalErrors;
            w.printf("%s,%d,%d,%d,%d,%d,%d,%s%n",
                    spsName, result.tableId, rows.size(), cleanRows, totalErrors,
                    result.executableRules, result.totalRules, "COMPLETED");
        }

        // sps_check_error_detail: each row in each error output group
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                reportDir.resolve("sps_check_error_detail.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("run_batch,row_key,rule_name,output_name,field_values");
            for (SpssOutputRule or : outputRules) {
                if (or.getSheetName().contains("清理后")) continue;
                for (RowContext row : rows) {
                    if (or.matches(row)) {
                        StringBuilder fields = new StringBuilder();
                        for (Map.Entry<String, Object> e : row.getValues().entrySet()) {
                            if (fields.length() > 0) fields.append(";");
                            fields.append(e.getKey()).append("=").append(e.getValue());
                        }
                        w.printf("%s,%s,\"%s\",\"%s\",\"%s\"%n",
                                spsName, row.getSampleKey(), "",
                                or.getSheetName(), fields.toString());
                    }
                }
            }
        }

        System.out.printf("  Execution report: %d error rows written%n", totalErrors);
    }

    // ── Summary ──

    private static void writeSummaryReport(Path runDir, List<SpsRunResult> results) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                runDir.resolve("_summary.csv"), StandardCharsets.UTF_8))) {
            w.print('﻿');
            w.println("sps_name,status,total_rules,executable,unexecutable,dataset_rules,output_rules," +
                    "answers,rows,mappings,unsupported_types,skipped");
            for (SpsRunResult r : results) {
                w.printf("%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s%n",
                        r.spsName,
                        r.skipped ? "SKIPPED" : (r.unexecutableRules == 0 ? "FULL" : "PARTIAL"),
                        r.totalRules, r.executableRules, r.unexecutableRules,
                        r.totalDatasetRules, r.totalOutputRules,
                        r.totalAnswers, r.totalRows, r.totalMappings,
                        r.unsupportedStatements.size(),
                        r.skipped ? "yes" : "no");
            }
        }
        System.out.println("────────────────────────────────────────────");
        System.out.println("Summary written to " + runDir.resolve("_summary.csv"));
        System.out.println();
        for (SpsRunResult r : results) {
            String flag = r.skipped ? " SKIP" : (r.unexecutableRules == 0 ? " FULL" : " PART");
            System.out.printf("  %s %s  rules=%d/%d  rows=%d  outputs=%d  unsupported=%d%n",
                    flag, r.spsName, r.executableRules, r.totalRules,
                    r.totalRows, r.totalOutputRules, r.unsupportedStatements.size());
        }
    }

    // ── Result holder ──

    static class SpsRunResult {
        String spsName;
        long tableId;
        boolean skipped;

        int spsSize;
        int totalRules, totalDatasetRules, totalOutputRules;
        int totalAnswers, totalMappings, totalRows;
        long executableRules, unexecutableRules;

        List<String> unsupportedStatements = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();
    }
}
