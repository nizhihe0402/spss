package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.engine.executor.RuleExecutor;
import com.gxaysoft.project.spsscheck.engine.model.*;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.engine.executor.AvailabilityChecker;
import com.gxaysoft.project.spsscheck.execution.DbRuleExecutionDataLoader;
import com.gxaysoft.project.spsscheck.execution.RuleCorrectionRuntimeService;
import com.gxaysoft.project.spsscheck.io.*;
import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ExecutionService {
    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
    private final JdbcTemplate jdbc;
    private final ScriptService scriptService;
    private final RuleExecutionPersistenceService persistenceService;

    public ExecutionService(JdbcTemplate jdbc, ScriptService scriptService,
                           RuleExecutionPersistenceService persistenceService) {
        this.jdbc = jdbc;
        this.scriptService = scriptService;
        this.persistenceService = persistenceService;
    }

    public Map<String, Object> executeFromUpload(byte[] csvBytes, Long scriptId, Long tableId,
                                                  byte[] mappingBytes, byte[] studentBytes,
                                                  boolean strictValidate) throws Exception {
        PrototypeFileReaders.AnswerCsvLoadResult csvLoad = PrototypeFileReaders.readAnswerCsvDetailed(csvBytes);
        return executeCore(csvLoad, scriptId, tableId, mappingBytes, studentBytes, strictValidate, null);
    }

    public Map<String, Object> executeFromDb(DbRuleExecutionDataLoader.Request request) {
        DbRuleExecutionDataLoader.LoadResult loaded = new DbRuleExecutionDataLoader(jdbc).load(request);
        try {
            Map<String, Object> result = executeCore(loaded.getCsvLoad(), request.scriptId,
                    request.tableId, null, null, false, null);
            // Persist results — best effort, don't lose execution result on failure
            if (result.get("data") instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> spssResult = (Map<String, Object>) result.get("data");
                    // 构建纠偏所需的 变量→questionId 映射（从执行时 mappings）
                    @SuppressWarnings("unchecked")
                    Map<String, Long> varToQid = (Map<String, Long>) result.getOrDefault(
                        "_varToQid", Collections.emptyMap());
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, String>> correctionCleanValues =
                        (Map<String, Map<String, String>>) result.getOrDefault(
                            "_correctionCleanValues", Collections.emptyMap());
                    RuleExecutionPersistenceService.SaveSummary saveResult =
                        persistenceService.saveDbExecutionResult(
                                request, loaded.getAnswerTable(), loaded.getCsvLoad(),
                                spssResult, correctionCleanValues, varToQid);
                    result.put("persist", saveResult.toMap());
                } catch (Exception pe) {
                    log.error("保存执行结果到_clean/_fail失败", pe);
                    result.put("persistError", pe.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("DB执行失败", e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 500);
            err.put("msg", "执行规则失败: " + e.getMessage());
            return err;
        }
    }

    private Map<String, Object> executeCore(PrototypeFileReaders.AnswerCsvLoadResult csvLoad,
                                             Long scriptId, Long tableId,
                                             byte[] mappingBytes, byte[] studentBytes,
                                             boolean strictValidate,
                                             Map<String, Object> extra) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();

        AnswerDataValidationReport validationReport = new AnswerDataValidator(jdbc).validate(csvLoad, false);
        Map<String, Object> fieldValidation = new StudentValidationResultBuilder(jdbc).build(csvLoad, validationReport);

        if (strictValidate && !validationReport.isPassed()) {
            result.put("code", 2);
            result.put("msg", "数据字段校验未通过");
            result.put("data", fieldValidation);
            return result;
        }

        List<AnswerRecord> answers = csvLoad.getAnswers();
        long detectedTableId = TableIdDetector.detectMostFrequentTableId(answers);

        String spsText = scriptService.loadScriptContent(scriptId);
        Map<String, QuestionMapping> mappings = loadMappings(mappingBytes, scriptId);

        StudentInfoEnricher.LoadResult dbStudentData = StudentInfoEnricher.load(
                jdbc.getDataSource(), StudentInfoEnricher.collectStudentIds(answers));
        mappings.putAll(dbStudentData.mappings);

        ParsedScript parsed = SpssParser.parse(spsText);
        List<Rule> rules = parsed.getRules();
        List<DatasetRule> datasetRules = parsed.getDatasetRules();
        List<OutputRule> outputRules = parsed.getOutputRules();

        List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
        StudentInfoEnricher.enrichRows(rows, dbStudentData.studentInfo);

        RuleCorrectionRuntimeService.CorrectionResult correctionResult =
                new RuleCorrectionRuntimeService(jdbc).apply(scriptId, detectedTableId, rows, dbStudentData.studentInfo);

        RuleExecutor.execute(rows, rules);
        for (DatasetRule dr : datasetRules) dr.execute(rows);

        Map<String, Object> spssResult = StudentSpssRuleResultBuilder.build(
                rows, rules, answers, csvLoad.getStudentNamesByKey());

        // 构建纠偏所需的 变量→questionId 映射（从运行时 mappings）
        Map<String, Long> varToQid = buildVarToQidMap(mappings);

        result.put("code", 0);
        result.put("msg", "SPS 规则执行完成");
        result.put("scriptId", scriptId);
        result.put("dataTableId", detectedTableId);
        result.put("data", spssResult);
        result.put("fieldValidation", fieldValidation);
        result.put("_correctionCleanValues", correctionResult.getCleanValues());
        result.put("_varToQid", varToQid);
        if (extra != null) result.putAll(extra);
        return result;
    }

    /**
     * 从运行时 QuestionMapping 构建 变量名→questionId 映射。
     */
    private Map<String, Long> buildVarToQidMap(Map<String, QuestionMapping> mappings) {
        Map<String, Long> varToQid = new LinkedHashMap<>();
        if (mappings != null) {
            for (Map.Entry<String, QuestionMapping> e : mappings.entrySet()) {
                if (e.getValue() != null && e.getValue().getQuestionId() > 0) {
                    varToQid.put(e.getKey().trim().toUpperCase(), e.getValue().getQuestionId());
                }
            }
        }
        return varToQid;
    }

    private Map<String, QuestionMapping> loadMappings(byte[] mappingBytes, long scriptId) throws Exception {
        if (mappingBytes == null || mappingBytes.length == 0) {
            return scriptService.loadVariableMappings(scriptId);
        }
        String text = PrototypeFileReaders.decodeTextAuto(mappingBytes);
        return com.gxaysoft.project.spsscheck.parser.QuestionSqlParser.parseQuestionMappings(text, -1L);
    }
}
