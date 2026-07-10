package com.gxaysoft.project.spsscheck.v2.executor;

import com.gxaysoft.project.spsscheck.v2.handler.*;
import com.gxaysoft.project.spsscheck.v2.model.*;
import com.gxaysoft.project.spsscheck.v2.parser.BlockParser;
import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.io.*;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.parser.*;
import com.gxaysoft.project.spsscheck.v1.parser.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * V2 engine: SPS text → blocks → RuleType → RuleDefinition → Handler → execute.
 * Replaces the Step-based parsing approach.
 */
public final class BlockExecutor {
    private static final Logger log = LoggerFactory.getLogger(BlockExecutor.class);

    public static class RunResult {
        public String spsName;
        public long tableId;
        public int totalAnswers, totalMappings, totalRows;
        public List<RuleDefinition> rules = new ArrayList<>();
        public List<SpssOutputRule> outputRules = new ArrayList<>();
        public List<SpssDatasetRule> datasetRules = new ArrayList<>();
        public Map<RuleType, Integer> typeCounts = new LinkedHashMap<>();
        public int skipped;
    }

    /**
     * Full pipeline: parse → classify → execute → output.
     */
    public static RunResult run(Path spsPath, Path mappingPath, Path csvPath, Path studentPath) throws Exception {
        String spsName = spsPath.getFileName().toString().replace(".sps", "");
        log.info("开始执行: sps={}", spsName);
        String spsText = PrototypeFileReaders.readSpssText(spsPath);

        RunResult result = new RunResult();
        result.spsName = spsName;

        // 1. Parse → RuleDefinitions
        result.rules = BlockParser.parse(spsText);

        // Legacy: also parse output/dataset rules
        result.outputRules = SpssRuleParser.parseOutputRules(spsText);
        result.datasetRules = SpssRuleParser.parseDatasetRules(spsText);

        // Count by type
        for (RuleDefinition r : result.rules) {
            result.typeCounts.merge(r.getType(), 1, Integer::sum);
        }

        // 2. Load data (if CSV provided)
        if (csvPath == null || !Files.exists(csvPath)) {
            result.skipped = 1;
            return result;
        }

        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvPath);
        long tableId = TableIdDetector.detectMostFrequentTableId(answers);
        result.tableId = tableId;
        result.totalAnswers = answers.size();

        Map<String, QuestionMapping> mappings = loadMappings(mappingPath, tableId);

        // Student info
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

        // 3. Execute via handlers
        DuplicateMarkHandler dupHandler = null;
        for (RuleDefinition rule : result.rules) {
            if (rule.getType() == RuleType.DUPLICATE_MARK) {
                dupHandler = (DuplicateMarkHandler) HandlerRegistry.get(RuleType.DUPLICATE_MARK);
                continue;
            }
            RuleHandler handler = HandlerRegistry.get(rule.getType());
            if (handler == null) continue;
            for (RowContext row : rows) {
                handler.execute(rule, row);
            }
        }
        if (dupHandler != null) {
            dupHandler.executeOnDataset(rows, "PrimaryFirst1", "PrimaryLast");
        }

        // Dataset rules
        for (SpssDatasetRule dr : result.datasetRules) {
            dr.execute(rows);
        }

        return result;
    }

    /** Print a report of the run */
    public static void printReport(RunResult r) {
        System.out.printf("%n─── %s ───%n", r.spsName);
        System.out.printf("  Blocks: %d rules%n", r.rules.size());
        for (Map.Entry<RuleType, Integer> e : r.typeCounts.entrySet()) {
            System.out.printf("    %-25s %d%n", e.getKey().label, e.getValue());
        }
        if (r.skipped > 0) {
            System.out.println("  (no data — skipped execution)");
            return;
        }
        System.out.printf("  Data: tableId=%d, %d answers, %d mappings, %d rows%n",
                r.tableId, r.totalAnswers, r.totalMappings, r.totalRows);

        if (!r.outputRules.isEmpty()) {
            System.out.println("  Outputs:");
            for (SpssOutputRule or : r.outputRules) {
                int count = 0;
                // Would need rows reference — simplified here
                System.out.printf("    %s%n", or.getSheetName());
            }
        }
    }

    /** Write output CSVs and Excel-ready reports */
    public static void writeOutputs(Path outputDir, RunResult r, List<RowContext> rows) throws IOException {
        Path dir = outputDir.resolve(r.spsName);
        Files.createDirectories(dir);

        // Rule definition report
        try (java.io.PrintWriter w = new java.io.PrintWriter(
                Files.newBufferedWriter(dir.resolve("rules.csv"), java.nio.charset.StandardCharsets.UTF_8))) {
            w.println("type,target,description,sources,expression");
            for (RuleDefinition rd : r.rules) {
                w.printf("%s,%s,\"%s\",\"%s\",\"%s\"%n",
                        rd.getType(), rd.getTarget(),
                        rd.getDescription() != null ? rd.getDescription() : "",
                        String.join(";", rd.getSourceVariables()),
                        rd.getExpression() != null ? rd.getExpression() : "");
            }
        }

        // Output groups
        if (!r.outputRules.isEmpty() && rows != null) {
            Path outDir = dir.resolve("outputs");
            OutputWriter.writeOutputs(outDir, rows, r.outputRules);
        }
    }

    private static Map<String, QuestionMapping> loadMappings(Path path, long tableId) throws Exception {
        if (path.toString().endsWith(".json")) {
            return QuestionJsonParser.parseQuestionMappings(path, tableId);
        }
        String sql = PrototypeFileReaders.readSpssText(path);
        return QuestionSqlParser.parseQuestionMappings(sql, tableId);
    }
}
