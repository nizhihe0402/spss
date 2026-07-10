package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.engine.executor.AvailabilityChecker;
import com.gxaysoft.project.spsscheck.engine.executor.RuleExecutor;
import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.OutputRule;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.io.AnswerPivot;
import com.gxaysoft.project.spsscheck.io.OutputWriter;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.io.StudentInfoLoader;
import com.gxaysoft.project.spsscheck.io.TableIdDetector;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.parser.QuestionJsonParser;
import com.gxaysoft.project.spsscheck.parser.QuestionSqlParser;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrototypeCli {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java ... PrototypeCli <sps-file> <sql-or-json-file> <answer-csv> [tableId] [output-dir]");
            return;
        }

        Path spsPath = Paths.get(args[0]);
        Path sqlPath = Paths.get(args[1]);
        Path csvPath = Paths.get(args[2]);

        String spsText = PrototypeFileReaders.readSpssText(spsPath);
        String sqlText = PrototypeFileReaders.readSpssText(sqlPath);
        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvPath);

        long tableId;
        if (args.length >= 4) {
            tableId = Long.parseLong(args[3]);
        } else {
            tableId = TableIdDetector.detectMostFrequentTableId(answers);
        }

        Map<String, QuestionMapping> mappings;
        if (sqlPath.toString().endsWith(".json")) {
            mappings = QuestionJsonParser.parseQuestionMappings(sqlPath, tableId);
        } else {
            mappings = QuestionSqlParser.parseQuestionMappings(sqlText, tableId);
        }

        // Merge supplementary student-level variables (ZJTYPE, SFZ) into mappings
        Path studentInfoPath = Paths.get("docs/sources/data/学生证件类型证件号.json");
        StudentInfoLoader.LoadResult studentData = null;
        if (java.nio.file.Files.exists(studentInfoPath)) {
            studentData = StudentInfoLoader.load(studentInfoPath);
            mappings.putAll(studentData.mappings);
        }

        ParsedScript parsed = SpssParser.parse(spsText);
        List<Rule> rules = parsed.getRules();
        List<DatasetRule> datasetRules = parsed.getDatasetRules();
        List<OutputRule> outputRules = parsed.getOutputRules();

        List<RowContext> rows = AnswerPivot.pivot(answers, mappings);

        if (studentData != null) {
            StudentInfoLoader.enrichRows(rows, studentData.studentInfo);
        }

        RuleExecutor.execute(rows, rules);
        for (DatasetRule datasetRule : datasetRules) {
            datasetRule.execute(rows);
        }

        // Availability check
        java.util.Set<String> dbColumns = new java.util.LinkedHashSet<String>();
        for (String variable : mappings.keySet()) {
            dbColumns.add(SpssUtil.normalize(variable));
        }
        AvailabilityChecker checker = new AvailabilityChecker(dbColumns);
        for (DatasetRule dr : datasetRules) {
            checker.addDatasetVariables(dr.getFirstVariable(), dr.getLastVariable());
        }
        List<Rule> availableRules = checker.filterAvailable(rules);
        int executable = availableRules.size();
        int unexecutable = rules.size() - executable;

        System.out.println("tableId=" + tableId);
        System.out.println("answers=" + answers.size());
        System.out.println("mappings=" + mappings.size());
        System.out.println("rows=" + rows.size());
        System.out.println("rules=" + rules.size());
        System.out.println("datasetRules=" + datasetRules.size());
        System.out.println("outputRules=" + outputRules.size());
        System.out.println("executableRules=" + executable);
        if (unexecutable > 0) {
            System.out.println("unexecutableRules=" + unexecutable);
        }

        Path outputDir = args.length >= 5 ? Paths.get(args[4]) : Paths.get("output");
        System.out.println("outputs:");
        for (OutputRule outputRule : outputRules) {
            int count = 0;
            for (RowContext row : rows) {
                if (outputRule.matches(row)) {
                    count++;
                }
            }
            System.out.println("  " + outputRule.getSheetName() + " = " + count);
        }
        OutputWriter.writeOutputs(outputDir, rows, outputRules);
    }
}
