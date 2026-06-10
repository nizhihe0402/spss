package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.io.*;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.parser.QuestionJsonParser;
import com.gxaysoft.project.spsscheck.v2.handler.*;
import com.gxaysoft.project.spsscheck.v2.model.*;
import com.gxaysoft.project.spsscheck.v2.parser.BlockParser;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidationReport;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidator;
import com.gxaysoft.project.spsscheck.validation.StudentValidationResultBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class RunController {

    @Autowired
    private JdbcTemplate jdbc;


    /**
     * Validate uploaded bus_doctor_answer CSV without executing SPSS rules.
     * This endpoint is useful when you only want to check table_id/project_id/year/del_flag,
     * question_id, option_id, and content correctness.
     */
    @PostMapping("/validate/answers")
    public Map<String, Object> validateAnswers(@RequestParam("file") MultipartFile csvFile) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            PrototypeFileReaders.AnswerCsvLoadResult csvLoad = PrototypeFileReaders.readAnswerCsvDetailed(csvFile.getBytes());
            AnswerDataValidationReport validationReport = new AnswerDataValidator(jdbc).validate(csvLoad);
            Map<String, Object> splitResult = new StudentValidationResultBuilder(jdbc).build(csvLoad, validationReport);
            result.put("code", 0);
            result.put("msg", validationReport.isPassed() ? "数据字段校验通过" : "校验完成，存在未通过学生");
            result.put("data", splitResult);
            result.put("splitResult", splitResult);
            result.put("validationReport", validationReport.toMap(500));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.put("code", 1);
            result.put("msg", "校验失败: " + e.getMessage());
            result.put("trace", sw.toString());
        }
        return result;
    }

    /**
     * Execute a script's rules against uploaded CSV answer data.
     * Accepts: scriptId, CSV file, optional mapping file (JSON/SQL), optional student JSON.
     */
    @PostMapping("/run/{scriptId}")
    public Map<String, Object> run(@PathVariable Long scriptId,
                                   @RequestParam("file") MultipartFile csvFile,
                                   @RequestParam(value = "mapping", required = false) MultipartFile mappingFile,
                                   @RequestParam(value = "student", required = false) MultipartFile studentFile,
                                   @RequestParam(value = "strictValidate", defaultValue = "false") boolean strictValidate) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // 1. Load script from DB
            Map<String, Object> script = jdbc.queryForMap(
                    "SELECT id, script_name, script_content FROM sps_script WHERE id=?", scriptId);

            // 2. Parse with V2 engine (classification + V1 execution steps merged)
            String spsText = String.valueOf(script.get("script_content"));
            List<RuleDefinition> v2Rules = BlockParser.parse(spsText);
            result.put("scriptName", script.get("script_name"));
            result.put("totalRules", v2Rules.size());

            // 3. Parse and validate CSV answer data.
            //    The uploaded file is usually exported from bus_doctor_answer and may be GBK/GB18030.
            //    Do not decode it as UTF-8 blindly.  The validator checks table_id/project_id/year/del_flag,
            //    question_id-table_id consistency, option_id validity, content-option.code consistency,
            //    duplicates and required-question coverage before rule execution.
            PrototypeFileReaders.AnswerCsvLoadResult csvLoad = PrototypeFileReaders.readAnswerCsvDetailed(csvFile.getBytes());
            List<AnswerRecord> answers = csvLoad.getAnswers();
            AnswerDataValidationReport validationReport = new AnswerDataValidator(jdbc).validate(csvLoad);
            Map<String, Object> splitResult = new StudentValidationResultBuilder(jdbc).build(csvLoad, validationReport);
            result.put("validationReport", validationReport.toMap(300));
            result.put("data", splitResult);
            result.put("splitResult", splitResult);
            if (strictValidate && !validationReport.isPassed()) {
                result.put("code", 2);
                result.put("msg", "数据字段校验未通过，已停止执行规则");
                return result;
            }

            long tableId = TableIdDetector.detectMostFrequentTableId(answers);

            // 4. Load question mappings
            Map<String, QuestionMapping> mappings = new LinkedHashMap<>();
            if (mappingFile != null && !mappingFile.isEmpty()) {
                String mapText = new String(mappingFile.getBytes(), "UTF-8");
                if (mappingFile.getOriginalFilename().endsWith(".json")) {
                    Path tmpJson = Files.createTempFile("spss_map_", ".json");
                    Files.write(tmpJson, mappingFile.getBytes());
                    mappings = QuestionJsonParser.parseQuestionMappings(tmpJson, tableId);
                    try { Files.delete(tmpJson); } catch (Exception ignored) {}
                } else {
                    mappings = com.gxaysoft.project.spsscheck.parser.QuestionSqlParser.parseQuestionMappings(mapText, tableId);
                }
            }

            // 5. Load student info if provided
            if (studentFile != null && !studentFile.isEmpty()) {
                Path tmpStu = Files.createTempFile("spss_stu_", ".json");
                Files.write(tmpStu, studentFile.getBytes());
                StudentInfoLoader.LoadResult sd = StudentInfoLoader.load(tmpStu);
                mappings.putAll(sd.mappings);
                try { Files.delete(tmpStu); } catch (Exception ignored) {}
            }

            // 6. Pivot data
            List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
            if (rows.isEmpty()) {
                result.put("code", 1);
                result.put("msg", "没有匹配到任何数据行。tableId=" + tableId + " mappings=" + mappings.size());
                return result;
            }

            // 7. Execute: COMPUTE first (set intermediate values), then CHECK (use them)
            long startTime = System.currentTimeMillis();
            // Pass 1: COMPUTE_INTERMEDIATE (build intermediate variables)
            for (RuleDefinition rule : v2Rules) {
                if (rule.getType() != RuleType.COMPUTE_INTERMEDIATE) continue;
                RuleHandler handler = HandlerRegistry.get(rule.getType());
                if (handler == null) continue;
                for (RowContext row : rows) {
                    try { handler.execute(rule, row); } catch (Exception ignored) {}
                }
            }
            // Pass 2: All CHECK types (IDENTITY, MISSING, RANGE, CONSISTENCY, DOCUMENT, CONDITIONAL, OUTCOME)
            for (RuleDefinition rule : v2Rules) {
                if (rule.getType() == RuleType.COMPUTE_INTERMEDIATE
                        || rule.getType() == RuleType.DUPLICATE_MARK
                        || rule.getType() == RuleType.OUTPUT_GROUP) continue;
                RuleHandler handler = HandlerRegistry.get(rule.getType());
                if (handler == null) continue;
                for (RowContext row : rows) {
                    try { handler.execute(rule, row); } catch (Exception ignored) {}
                }
            }
            // Pass 3: DUPLICATE_MARK
            RuleHandler dupHandler = HandlerRegistry.get(RuleType.DUPLICATE_MARK);
            if (dupHandler != null) {
                ((DuplicateMarkHandler) dupHandler).executeOnDataset(rows, "PrimaryFirst1", "PrimaryLast");
            }
            long elapsed = System.currentTimeMillis() - startTime;

            // 8. Build result summary — use V2 RuleDefinitions
            result.put("code", 0);
            result.put("totalRows", rows.size());
            result.put("tableId", tableId);
            result.put("elapsedMs", elapsed);

            List<Map<String, Object>> ruleResults = new ArrayList<>();
            for (RuleDefinition r : v2Rules) {
                if (r.getType() == RuleType.COMPUTE_INTERMEDIATE
                        || r.getType() == RuleType.OUTPUT_GROUP) continue;
                String target = r.getTarget();
                int errorCount = 0;
                for (RowContext row : rows) {
                    if (row.getFlag(target) == 1) errorCount++;
                }
                Map<String, Object> rr = new LinkedHashMap<>();
                rr.put("target", target);
                rr.put("type", r.getType().name());
                rr.put("typeLabel", r.getType().label);
                rr.put("description", r.getDescription());
                rr.put("errorCount", errorCount);
                rr.put("totalRows", rows.size());
                rr.put("errorRate", rows.size() > 0 ? String.format("%.1f%%", 100.0 * errorCount / rows.size()) : "0%");
                ruleResults.add(rr);
            }
            result.put("ruleResults", ruleResults);

            // Error rows detail (first 100)
            List<Map<String, Object>> errorRows = new ArrayList<>();
            int shown = 0;
            for (RowContext row : rows) {
                if (shown >= 100) break;
                boolean hasError = false;
                for (RuleDefinition r : v2Rules) {
                    if (r.getType() == RuleType.COMPUTE_INTERMEDIATE) continue;
                    if (row.getFlag(r.getTarget()) == 1) { hasError = true; break; }
                }
                if (hasError) {
                    shown++;
                    Map<String, Object> er = new LinkedHashMap<>();
                    er.put("sampleKey", row.getSampleKey());
                    Map<String, String> rowFlags = new LinkedHashMap<>();
                    for (RuleDefinition r : v2Rules) {
                        if (r.getType() == RuleType.COMPUTE_INTERMEDIATE) continue;
                        int f = row.getFlag(r.getTarget());
                        if (f == 1) rowFlags.put(r.getTarget(), r.getDescription() != null ? r.getDescription() : "异常");
                    }
                    er.put("flags", rowFlags);
                    errorRows.add(er);
                }
            }
            result.put("errorRows", errorRows);
            result.put("totalErrorRows", shown >= 100 ? "100+" : String.valueOf(shown));

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.put("code", 1);
            result.put("msg", "执行失败: " + e.getMessage());
            result.put("trace", sw.toString());
        }
        return result;
    }

}
