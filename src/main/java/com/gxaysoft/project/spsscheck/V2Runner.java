package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.engine.executor.RuleExecutor;
import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.OutputRule;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.io.*;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Engine runner: scans all .sps files, parses with SpssParser, executes with RuleExecutor.
 *
 * Usage: java ... V2Runner [sps-dir] [mapping-json] [csv-data-dir]
 */
public class V2Runner {
    private static final Logger log = LoggerFactory.getLogger(V2Runner.class);

    public static void main(String[] args) throws Exception {
        Path spsDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");
        Path mapping = args.length >= 2 ? Paths.get(args[1])
                : Paths.get(System.getProperty("mappingPath", "docs/sources/sql/bus_question_202606081429.json"));
        Path csvDir = args.length >= 3 ? Paths.get(args[2]) : Paths.get("docs/sources/cvs");
        Path student = Paths.get(System.getProperty("studentPath", "docs/sources/data/学生证件类型证件号.json"));

        List<Path> spsFiles = Files.list(spsDir)
                .filter(p -> p.toString().endsWith(".sps") && !p.getFileName().toString().startsWith("修"))
                .sorted().collect(Collectors.toList());

        // Summary
        log.info("=== SPS Run (Unified Engine) ===");
        Map<RuleType, Integer> globalCounts = new LinkedHashMap<>();
        int totalRules = 0;

        for (Path spsFile : spsFiles) {
            RunResult r = run(spsFile, mapping, findCsv(csvDir, spsFile), student);
            printReport(r);

            totalRules += r.rules.size();
            for (Map.Entry<RuleType, Integer> e : r.typeCounts.entrySet()) {
                globalCounts.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }

        log.info("");
        log.info("═══════════════════════════════════");
        log.info("Total: {} rules across {} tables", totalRules, spsFiles.size());
        log.info("By type:");
        for (Map.Entry<RuleType, Integer> e : globalCounts.entrySet()) {
            log.info("  {}{}", String.format("%-25s", e.getKey().label), e.getValue());
        }
    }

    public static RunResult run(Path spsPath, Path mappingPath, Path csvPath, Path studentPath) throws Exception {
        String spsName = spsPath.getFileName().toString().replace(".sps", "");
        String spsText = PrototypeFileReaders.readSpssText(spsPath);

        RunResult result = new RunResult();
        result.spsName = spsName;

        // 1. Parse
        ParsedScript parsed = SpssParser.parse(spsText);
        result.rules = parsed.getRules();
        result.outputRules = parsed.getOutputRules();
        result.datasetRules = parsed.getDatasetRules();

        for (Rule r : result.rules) {
            RuleType rt = r.getType() != null ? r.getType() : RuleType.CONDITIONAL_BLOCK;
            result.typeCounts.merge(rt, 1, Integer::sum);
        }

        // 2. Load data
        if (csvPath == null || !Files.exists(csvPath)) {
            result.skipped = 1;
            return result;
        }

        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvPath);
        long tableId = TableIdDetector.detectMostFrequentTableId(answers);
        result.tableId = tableId;
        result.totalAnswers = answers.size();

        Map<String, QuestionMapping> mappings = loadMappings(mappingPath, tableId);

        if (studentPath != null && Files.exists(studentPath)) {
            StudentInfoLoader.LoadResult sd = StudentInfoLoader.load(studentPath);
            mappings.putAll(sd.mappings);
        }
        result.totalMappings = mappings.size();

        List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
        result.totalRows = rows.size();

        if (rows.isEmpty()) {
            result.skipped = 1;
            return result;
        }

        // 3. Execute
        RuleExecutor.execute(rows, result.rules);
        for (DatasetRule dr : result.datasetRules) {
            dr.execute(rows);
        }

        return result;
    }

    public static void printReport(RunResult r) {
        log.info("");
        log.info("─── {} ───", r.spsName);
        log.info("  Rules: {}", r.rules.size());
        for (Map.Entry<RuleType, Integer> e : r.typeCounts.entrySet()) {
            log.info("    {}{}", String.format("%-25s", e.getKey().label), e.getValue());
        }
        if (r.skipped > 0) {
            log.info("  (no data — skipped execution)");
            return;
        }
        log.info("  Data: tableId={}, {} answers, {} mappings, {} rows",
                r.tableId, r.totalAnswers, r.totalMappings, r.totalRows);

        if (!r.outputRules.isEmpty()) {
            log.info("  Outputs:");
            for (OutputRule or : r.outputRules) {
                log.info("    {}", or.getSheetName());
            }
        }
    }

    private static Path findCsv(Path csvDir, Path spsFile) throws Exception {
        String name = spsFile.getFileName().toString().replace(".sps", "");
        Path dir = csvDir.resolve(name);
        if (!Files.isDirectory(dir)) return null;
        return Files.list(dir)
                .filter(p -> p.toString().endsWith(".csv") && !p.toString().contains("intervene"))
                .findFirst().orElse(null);
    }

    private static Map<String, QuestionMapping> loadMappings(Path path, long tableId) throws Exception {
        if (path.toString().endsWith(".json")) {
            return QuestionJsonParser.parseQuestionMappings(path, tableId);
        }
        String sql = PrototypeFileReaders.readSpssText(path);
        return QuestionSqlParser.parseQuestionMappings(sql, tableId);
    }

    public static class RunResult {
        public String spsName;
        public long tableId;
        public int totalAnswers, totalMappings, totalRows;
        public List<Rule> rules = new ArrayList<>();
        public List<OutputRule> outputRules = new ArrayList<>();
        public List<DatasetRule> datasetRules = new ArrayList<>();
        public Map<RuleType, Integer> typeCounts = new LinkedHashMap<>();
        public int skipped;
    }
}
