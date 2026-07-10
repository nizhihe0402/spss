package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.engine.executor.RuleExecutor;
import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.io.*;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.parser.QuestionJsonParser;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidationReport;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidator;
import com.gxaysoft.project.spsscheck.validation.StudentValidationResultBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class RunController {
    private static final Logger log = LoggerFactory.getLogger(RunController.class);

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
            log.error("校验失败", e);
            result.put("code", 1);
            result.put("msg", "校验失败: " + e.getMessage());
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

            // 2. Parse with unified engine
            String spsText = String.valueOf(script.get("script_content"));
            ParsedScript parsed = SpssParser.parse(spsText);
            List<Rule> rules = parsed.getRules();
            List<DatasetRule> datasetRules = parsed.getDatasetRules();
            result.put("scriptName", script.get("script_name"));
            result.put("totalRules", rules.size());

            // 3. Parse and validate CSV answer data.
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

            // 7. Execute: single-pass row-level execution (engine handles compute→check ordering)
            long startTime = System.currentTimeMillis();
            RuleExecutor.execute(rows, rules);
            for (DatasetRule dr : datasetRules) {
                dr.execute(rows);
            }
            long elapsed = System.currentTimeMillis() - startTime;

            // 8. Build result summary
            result.put("code", 0);
            result.put("totalRows", rows.size());
            result.put("tableId", tableId);
            result.put("elapsedMs", elapsed);

            List<Map<String, Object>> ruleResults = new ArrayList<>();
            for (Rule r : rules) {
                RuleType rt = r.getType() != null ? r.getType() : RuleType.CONDITIONAL_BLOCK;
                if (rt == RuleType.COMPUTE_INTERMEDIATE || rt == RuleType.OUTPUT_GROUP) continue;
                String target = r.getTarget();
                int errorCount = 0;
                for (RowContext row : rows) {
                    if (row.getFlag(target) == 1) errorCount++;
                }
                Map<String, Object> rr = new LinkedHashMap<>();
                rr.put("target", target);
                rr.put("type", rt.name());
                rr.put("typeLabel", rt.label);
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
                for (Rule r : rules) {
                    RuleType rt = r.getType() != null ? r.getType() : RuleType.CONDITIONAL_BLOCK;
                    if (rt == RuleType.COMPUTE_INTERMEDIATE) continue;
                    if (row.getFlag(r.getTarget()) == 1) { hasError = true; break; }
                }
                if (hasError) {
                    shown++;
                    Map<String, Object> er = new LinkedHashMap<>();
                    er.put("sampleKey", row.getSampleKey());
                    Map<String, String> rowFlags = new LinkedHashMap<>();
                    for (Rule r : rules) {
                        RuleType rt = r.getType() != null ? r.getType() : RuleType.CONDITIONAL_BLOCK;
                        if (rt == RuleType.COMPUTE_INTERMEDIATE) continue;
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
            log.error("执行失败", e);
            result.put("code", 1);
            result.put("msg", "执行失败: " + e.getMessage());
        }
        return result;
    }

}
