package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.execution.DbRuleExecutionDataLoader;
import com.gxaysoft.project.spsscheck.execution.RuleCorrectionRuntimeService;
import com.gxaysoft.project.spsscheck.io.AnswerPivot;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.io.StudentInfoEnricher;
import com.gxaysoft.project.spsscheck.io.StudentInfoLoader;
import com.gxaysoft.project.spsscheck.io.TableIdDetector;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.QuestionJsonParser;
import com.gxaysoft.project.spsscheck.parser.QuestionSqlParser;
import com.gxaysoft.project.spsscheck.parser.QuestionVariableNameSelector;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import com.gxaysoft.project.spsscheck.persistence.RuleExecutionPersistenceService;
import com.gxaysoft.project.spsscheck.persistence.ScriptQuestionMappingService;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidationReport;
import com.gxaysoft.project.spsscheck.validation.AnswerDataValidator;
import com.gxaysoft.project.spsscheck.validation.StudentSpssRuleResultBuilder;
import com.gxaysoft.project.spsscheck.validation.StudentValidationResultBuilder;
import com.gxaysoft.project.spsscheck.v1.executor.RuleEngine;
import com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule;
import com.gxaysoft.project.spsscheck.v1.model.SpssDatasetRule;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/rules")
public class RuleExecuteV2Controller {

    @Autowired
    private JdbcTemplate jdbc;

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestParam(value = "csvFile", required = false) MultipartFile csvFile,
                                       @RequestParam(value = "file", required = false) MultipartFile file,
                                       @RequestParam(value = "mappingFile", required = false) MultipartFile mappingFile,
                                       @RequestParam(value = "studentFile", required = false) MultipartFile studentFile,
                                       @RequestParam(value = "scriptId", required = false) Long scriptId,
                                       @RequestParam(value = "tableId", required = false) Long tableId,
                                       @RequestParam(value = "table_id", required = false) Long tableId2,
                                       @RequestParam(value = "projectId", required = false) Long projectId,
                                       @RequestParam(value = "project_id", required = false) Long projectId2,
                                       @RequestParam(value = "year", required = false) String year,
                                       @RequestParam(value = "fieldCheck", required = false) String fieldCheck,
                                       @RequestParam(value = "strictValidate", required = false, defaultValue = "false") boolean strictValidate) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            MultipartFile actualFile = csvFile != null ? csvFile : file;
            if (actualFile == null || actualFile.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "请上传 CSV 数据文件");
                return result;
            }

            Long actualTableId = tableId != null ? tableId : tableId2;
            Long actualProjectId = projectId != null ? projectId : projectId2;

            PrototypeFileReaders.AnswerCsvLoadResult csvLoad =
                    PrototypeFileReaders.readAnswerCsvDetailed(actualFile.getBytes());
            applyDefaults(csvLoad, actualTableId, actualProjectId, year);

            Map<String, Object> response = executeLoaded(csvLoad, mappingFile, studentFile, scriptId, strictValidate, new LinkedHashMap<String, Object>());
            response.remove("_correctionCleanValues");
            return response;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.put("code", 500);
            result.put("msg", "执行规则失败: " + e.getMessage());
            result.put("trace", sw.toString());
            return result;
        }
    }

    @PostMapping("/execute-db")
    public Map<String, Object> executeDb(@RequestParam(value = "scriptId", required = false) Long scriptId,
                                         @RequestParam(value = "projectId", required = false) Long projectId,
                                         @RequestParam(value = "project_id", required = false) Long projectId2,
                                         @RequestParam(value = "tableId", required = false) Long tableId,
                                         @RequestParam(value = "table_id", required = false) Long tableId2,
                                         @RequestParam(value = "divisionId", required = false) Long divisionId,
                                         @RequestParam(value = "division_id", required = false) Long divisionId2,
                                         @RequestParam(value = "schoolId", required = false) Long schoolId,
                                         @RequestParam(value = "school_id", required = false) Long schoolId2,
                                         @RequestParam(value = "year", required = false) String year,
                                         @RequestParam(value = "source", required = false, defaultValue = "normal") String source,
                                         @RequestParam(value = "strictValidate", required = false, defaultValue = "false") boolean strictValidate) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            DbRuleExecutionDataLoader.Request request = new DbRuleExecutionDataLoader.Request();
            request.scriptId = scriptId == null ? -1L : scriptId.longValue();
            request.projectId = firstPositive(projectId, projectId2);
            request.tableId = firstPositive(tableId, tableId2);
            request.divisionId = firstPositive(divisionId, divisionId2);
            request.schoolId = firstPositive(schoolId, schoolId2);
            request.year = year;
            request.source = source;

            DbRuleExecutionDataLoader.LoadResult loaded = new DbRuleExecutionDataLoader(jdbc).load(request);
            Map<String, Object> extra = new LinkedHashMap<String, Object>();
            extra.put("executionSource", "db");
            extra.put("answerTable", loaded.getAnswerTable());
            extra.put("projectId", request.projectId);
            extra.put("tableId", request.tableId);
            extra.put("divisionId", request.divisionId);
            extra.put("schoolId", request.schoolId);
            extra.put("source", source == null || source.trim().isEmpty() ? "normal" : source.trim());
            extra.put("answerCount", loaded.getCsvLoad().getAnswers().size());
            Map<String, Object> response = executeLoaded(loaded.getCsvLoad(), null, null, scriptId, strictValidate, extra);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> correctionCleanValues =
                    response.get("_correctionCleanValues") instanceof Map
                            ? (Map<String, Map<String, String>>) response.remove("_correctionCleanValues")
                            : new LinkedHashMap<String, Map<String, String>>();
            if (asLong(response.get("code"), -1L) == 0L && response.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> spssResult = (Map<String, Object>) response.get("data");
                RuleExecutionPersistenceService.SaveSummary saveSummary =
                        new RuleExecutionPersistenceService(jdbc).saveDbExecutionResult(
                                request, loaded.getAnswerTable(), loaded.getCsvLoad(), spssResult, correctionCleanValues);
                response.put("persist", saveSummary.toMap());
            }
            return response;
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            return result;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.put("code", 500);
            result.put("msg", "执行规则失败: " + e.getMessage());
            result.put("trace", sw.toString());
            return result;
        }
    }

    private Map<String, Object> executeLoaded(PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                              MultipartFile mappingFile,
                                              MultipartFile studentFile,
                                              Long scriptId,
                                              boolean strictValidate,
                                              Map<String, Object> extra) throws Exception {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            AnswerDataValidationReport validationReport = new AnswerDataValidator(jdbc).validate(csvLoad, false);
            Map<String, Object> fieldValidationResult =
                    new StudentValidationResultBuilder(jdbc).build(csvLoad, validationReport);
            if (strictValidate && !validationReport.isPassed()) {
                result.put("code", 2);
                result.put("msg", "数据字段校验未通过，已停止执行 SPS 规则");
                result.put("data", fieldValidationResult);
                result.put("validationReport", validationReport.toMap(300));
                return result;
            }
            if (scriptId == null || scriptId.longValue() <= 0) {
                result.put("code", 400);
                result.put("msg", "请选择要执行的 SPS 脚本");
                result.put("data", fieldValidationResult);
                result.put("validationReport", validationReport.toMap(300));
                return result;
            }

            List<AnswerRecord> answers = csvLoad.getAnswers();
            long detectedTableId = TableIdDetector.detectMostFrequentTableId(answers);
            String spsText = loadScriptContent(scriptId);
            Map<String, QuestionMapping> mappings = loadMappings(mappingFile, scriptId);

            StudentInfoEnricher.LoadResult dbStudentData = loadStudentDataFromDb(answers);
            mappings.putAll(dbStudentData.mappings);
            StudentInfoLoader.LoadResult bundledStudentData = loadBundledStudentData();
            if (bundledStudentData != null) {
                mappings.putAll(bundledStudentData.mappings);
                StudentInfoEnricher.mergeMissingStudentInfo(dbStudentData.studentInfo, bundledStudentData.studentInfo);
            }
            StudentInfoLoader.LoadResult studentData = loadStudentData(studentFile);
            if (studentData != null) {
                mappings.putAll(studentData.mappings);
                StudentInfoEnricher.mergeMissingStudentInfo(dbStudentData.studentInfo, studentData.studentInfo);
            }

            List<SpssCheckRule> rules = SpssRuleParser.parseRules(spsText);
            List<SpssDatasetRule> datasetRules = SpssRuleParser.parseDatasetRules(spsText);
            List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
            StudentInfoEnricher.enrichRows(rows, dbStudentData.studentInfo);
            RuleCorrectionRuntimeService.CorrectionResult correctionResult =
                    new RuleCorrectionRuntimeService(jdbc).apply(scriptId, detectedTableId, rows, dbStudentData.studentInfo);

            RuleEngine.execute(rows, rules);
            for (SpssDatasetRule datasetRule : datasetRules) {
                datasetRule.execute(rows);
            }

            Map<String, Object> spssResult = StudentSpssRuleResultBuilder.build(
                    rows, rules, answers, csvLoad.getStudentNamesByKey());

            result.put("code", 0);
            result.put("msg", "SPS 规则执行完成");
            result.put("scriptId", scriptId);
            result.put("dataTableId", detectedTableId);
            result.put("data", spssResult);
            result.put("fieldValidation", fieldValidationResult);
            result.put("validationReport", validationReport.toMap(300));
            result.put("_correctionCleanValues", correctionResult.getCleanValues());
            if (extra != null) {
                result.putAll(extra);
            }
            return result;
    }

    private void applyDefaults(PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                               Long tableId,
                               Long projectId,
                               String year) {
        if (csvLoad == null || csvLoad.getAnswers().isEmpty()) return;
        if ((tableId == null || tableId.longValue() <= 0)
                && (projectId == null || projectId.longValue() <= 0)
                && (year == null || year.trim().length() == 0)) {
            return;
        }
        for (int i = 0; i < csvLoad.getAnswers().size(); i++) {
            AnswerRecord a = csvLoad.getAnswers().get(i);
            long nextTableId = a.getTableId() > 0 ? a.getTableId() : (tableId == null ? a.getTableId() : tableId.longValue());
            long nextProjectId = a.getProjectId() > 0 ? a.getProjectId() : (projectId == null ? a.getProjectId() : projectId.longValue());
            String nextYear = !isBlank(a.getYear()) ? a.getYear() : year;
            AnswerRecord replaced = new AnswerRecord(
                    a.getRawId(),
                    a.getRowNumber(),
                    a.getSampleKey(),
                    a.getQuestionId(),
                    a.getOptionId(),
                    a.getStudentId(),
                    a.getContent(),
                    nextProjectId,
                    nextTableId,
                    a.getTimes(),
                    nextYear,
                    a.getDelFlag()
            );
            csvLoad.getAnswers().set(i, replaced);
        }
    }

    private String loadScriptContent(Long scriptId) {
        Map<String, Object> script = jdbc.queryForMap(
                "SELECT id, script_content FROM sps_script WHERE id=?", scriptId);
        Object value = script.get("script_content");
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, QuestionMapping> loadMappings(MultipartFile mappingFile, long scriptId) throws Exception {
        if (mappingFile != null && !mappingFile.isEmpty()) {
            Long scriptTableId = loadScriptTableId(scriptId);
            long tableId = scriptTableId == null ? -1L : scriptTableId.longValue();
            String filename = mappingFile.getOriginalFilename() == null ? "" : mappingFile.getOriginalFilename().toLowerCase();
            if (filename.endsWith(".json")) {
                Path tmp = Files.createTempFile("spss_mapping_", ".json");
                Files.write(tmp, mappingFile.getBytes());
                try {
                    return QuestionJsonParser.parseQuestionMappings(tmp, tableId);
                } finally {
                    try { Files.delete(tmp); } catch (Exception ignored) {}
                }
            }
            String text = PrototypeFileReaders.decodeTextAuto(mappingFile.getBytes());
            return QuestionSqlParser.parseQuestionMappings(text, tableId);
        }
        return new ScriptQuestionMappingService(jdbc).loadVariableMappings(scriptId);
    }

    private Long loadScriptTableId(long scriptId) {
        try {
            return jdbc.queryForObject("SELECT table_id FROM sps_script WHERE id=?", Long.class, scriptId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, QuestionMapping> loadMappingsFromDb(long tableId) {
        Map<String, QuestionMapping> mappings = new LinkedHashMap<String, QuestionMapping>();
        try {
            String sql = "SELECT question_id, table_id, content, export_content " +
                    "FROM bus_question WHERE table_id=? AND (del_flag IS NULL OR del_flag='0')";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, tableId);
            for (Map<String, Object> row : rows) {
                long questionId = asLong(row.get("question_id"), -1L);
                long actualTableId = asLong(row.get("table_id"), tableId);
                String variable = QuestionVariableNameSelector.variableNameFromExportContent(
                        stringValue(row.get("export_content")));
                if (questionId <= 0 || isBlank(variable)) continue;
                mappings.put(SpssUtil.normalize(variable),
                        new QuestionMapping(questionId, variable, stringValue(row.get("content")), actualTableId));
            }
        } catch (Exception ignored) {
            return mappings;
        }
        return mappings;
    }

    private StudentInfoEnricher.LoadResult loadStudentDataFromDb(List<AnswerRecord> answers) throws Exception {
        return StudentInfoEnricher.load(jdbc.getDataSource(), StudentInfoEnricher.collectStudentIds(answers));
    }

    private StudentInfoLoader.LoadResult loadBundledStudentData() throws Exception {
        Path path = Paths.get("docs", "sources", "data", "学生证件类型证件号.json");
        if (!Files.exists(path)) {
            return null;
        }
        return StudentInfoLoader.load(path);
    }

    private StudentInfoLoader.LoadResult loadStudentData(MultipartFile studentFile) throws Exception {
        if (studentFile == null || studentFile.isEmpty()) return null;
        Path tmp = Files.createTempFile("spss_student_", ".json");
        Files.write(tmp, studentFile.getBytes());
        try {
            return StudentInfoLoader.load(tmp);
        } finally {
            try { Files.delete(tmp); } catch (Exception ignored) {}
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long asLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            String text = String.valueOf(value).trim();
            if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
            return Long.parseLong(text);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private long firstPositive(Long first, Long second) {
        if (first != null && first.longValue() > 0) return first.longValue();
        if (second != null && second.longValue() > 0) return second.longValue();
        return -1L;
    }
}
