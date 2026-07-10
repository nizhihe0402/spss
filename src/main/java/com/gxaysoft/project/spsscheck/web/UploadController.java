package com.gxaysoft.project.spsscheck.web;

import com.gxaysoft.project.spsscheck.engine.model.OutputRule;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.persistence.SpsRepository;
import com.gxaysoft.project.spsscheck.persistence.ScriptQuestionMappingService;
import com.gxaysoft.project.spsscheck.persistence.RuleCorrectionPlan;
import com.gxaysoft.project.spsscheck.persistence.SourceQuestionMappingSyncService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UploadController {
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private JdbcTemplate jdbc;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "title", required = false) String title,
                                      @RequestParam(value = "tableId", required = false) Long tableId,
                                      @RequestParam(value = "table_id", required = false) Long tableId2,
                                      @RequestParam(value = "projectId", required = false) Long projectId,
                                      @RequestParam(value = "year", required = false) String year,
                                      @RequestParam(value = "projectType", required = false) String projectType) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            byte[] rawBytes = file.getBytes();
            // Use BOM-aware UTF-8 with GB18030 fallback (same as PrototypeFileReaders)
            String spsText = readSpsBytes(rawBytes);
            String name = extractScriptName(spsText, file.getOriginalFilename());
            long scriptTableId = resolveScriptTableId(tableId, tableId2, name);
            String scriptTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : name;
            Long scriptProjectId = (projectId != null && projectId.longValue() > 0) ? projectId : null;
            String scriptYear = (year != null && !year.trim().isEmpty()) ? year.trim() : null;
            String scriptProjectType = (projectType != null && !projectType.trim().isEmpty()) ? projectType.trim() : null;

            // Engine: unified parsing
            ParsedScript parsed = SpssParser.parse(spsText);
            List<Rule> rules = parsed.getRules();
            List<OutputRule> outputRules = parsed.getOutputRules();

            // Insert script with extended metadata
            KeyHolder keyHolder = new GeneratedKeyHolder();
            String insertSql = "INSERT INTO sps_script (script_name, script_content, table_code, table_id, " +
                "project_id, project_type, year, parse_status, version_no, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'PARSED', 1, 'DRAFT')";
            jdbc.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, scriptTitle);
                ps.setString(2, spsText);
                ps.setString(3, name);
                ps.setLong(4, scriptTableId);
                if (scriptProjectId != null) ps.setLong(5, scriptProjectId); else ps.setNull(5, java.sql.Types.BIGINT);
                ps.setString(6, scriptProjectType);
                ps.setString(7, scriptYear);
                return ps;
            }, keyHolder);
            long scriptId = keyHolder.getKey().longValue();
            insertScriptQuestionMappings(scriptId, scriptTableId);

            // Insert rules
            int sortNo = 0;
            for (Rule rd : rules) {
                sortNo++;
                insertRuleV2(scriptId, sortNo, rd);
            }
            // Insert output rules
            int outNo = 0;
            for (OutputRule or : outputRules) {
                outNo++;
                insertOutputRule(scriptId, outNo, or);
            }
            for (String[] stmt : SpsRepository.collectUnsupported(spsText)) {
                jdbc.update("INSERT INTO sps_unsupported_statement (script_id, statement_type, reason, risk_level) VALUES (?,?,?,?)",
                        scriptId, stmt[0], stmt[1], stmt[2]);
            }

            result.put("code", 0);
            result.put("scriptId", scriptId);
            result.put("tableId", scriptTableId);
            result.put("rules", rules.size());
            result.put("outputRules", outputRules.size());
            result.put("msg", "上传成功: " + rules.size() + " 条规则(V2)");
        } catch (Exception e) {
            log.error("解析失败", e);
            result.put("code", 1);
            result.put("msg", "解析失败: " + e.getMessage());
        }
        return result;
    }

    private void insertRuleV2(long scriptId, int sortNo, Rule rd) {
        String code = String.format("R%03d", sortNo);
        String sources = String.join(",", rd.getSourceVariables());
        String sourceQuestionMappings = new SourceQuestionMappingSyncService(jdbc).buildForSources(sources, loadScriptTableId(scriptId));
        RuleType rt = rd.getType() != null ? rd.getType() : RuleType.CONDITIONAL_BLOCK;
        RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                rt.name(), rd.getTarget(), sources, rd.getDescription());
        String spssSource = rd.getSpssSource() != null ? rd.getSpssSource() : "";
        String javaPreview = rd.getJavaPreview() != null ? rd.getJavaPreview() : "";
        jdbc.update(
            "INSERT INTO sps_rule (script_id, rule_code, rule_name, rule_type, target_variable, " +
            "source_variables, source_question_mappings, correction_enabled, correction_type, correction_variables, " +
            "correction_source, correction_strategy, correction_apply_stage, correction_write_clean, " +
            "correction_write_source, correction_description, spss_source, rule_json, java_preview, sort_no, affect_clean, warning_message) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            scriptId, code, rd.getTarget(), rt.name(),
            rd.getTarget(), sources, sourceQuestionMappings,
            correction.enabled ? 1 : 0, correction.type, correction.variables,
            correction.source, correction.strategy, correction.applyStage,
            correction.writeClean ? 1 : 0, correction.writeSource ? 1 : 0,
            correction.description,
            truncate(spssSource, 65535),
            "{\"v2\":true,\"type\":\"" + rt.name() + "\"}",
            javaPreview, sortNo,
            rt == RuleType.IDENTITY_CHECK
                    || rt == RuleType.MISSING_CHECK
                    || rt == RuleType.RANGE_CHECK
                    || rt == RuleType.CONSISTENCY_CHECK
                    || rt == RuleType.DOCUMENT_CHECK ? 1 : 0,
            rd.getDescription());
    }

    private long resolveScriptTableId(Long tableId, Long tableId2, String scriptName) {
        long requested = tableId != null && tableId.longValue() > 0 ? tableId.longValue()
                : (tableId2 != null && tableId2.longValue() > 0 ? tableId2.longValue() : -1L);
        return requested > 0 ? requested : ScriptQuestionMappingService.inferTableIdFromScriptName(scriptName);
    }

    private Long loadScriptTableId(long scriptId) {
        try {
            return jdbc.queryForObject("SELECT table_id FROM sps_script WHERE id=?", Long.class, scriptId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void insertScriptQuestionMappings(long scriptId, long tableId) throws Exception {
        if (tableId <= 0 || jdbc.getDataSource() == null) {
            return;
        }
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            new SpsRepository(conn).insertScriptQuestionMappings(
                    scriptId, ScriptQuestionMappingService.loadQuestionMappings(conn, tableId));
        }
    }

    private void insertOutputRule(long scriptId, int sortNo, OutputRule or) {
        String code = String.format("O%03d", sortNo);
        String type = or.getSheetName().contains("清理后") ? "CLEAN_DATA" : "ERROR_GROUP";
        jdbc.update("INSERT INTO sps_output_rule (script_id, output_code, output_name, output_type, " +
                "select_condition, spss_source, java_preview, sort_no) VALUES (?,?,?,?,?,?,?,?)",
                scriptId, code, or.getSheetName(), type,
                or.getCondition(), or.getSpssSource(), or.getJavaRule(), sortNo);
    }

    /** BOM-aware UTF-8 with GB18030 fallback (mirrors PrototypeFileReaders.readSpssText) */
    private static String readSpsBytes(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            return java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (Exception ex) {
            return new String(bytes, java.nio.charset.Charset.forName("GB18030"));
        }
    }

    /** Extract script name from multi-star comment or derive from filename */
    private static String extractScriptName(String spsText, String filename) {
        for (String line : spsText.split("[\\r\\n]+")) {
            String t = line.trim();
            if (t.matches("^\\*{3,}\\s*[^\\s].+") && !t.contains("Encoding")) {
                String cleaned = t.replaceFirst("^\\*+\\s*", "").trim();
                cleaned = cleaned.replaceAll("[.。]\\s*$", "");
                if (cleaned.length() >= 3 && cleaned.length() < 120) return cleaned;
            }
        }
        return filename.replace(".sps", "").replace(".SPS", "");
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) : s;
    }
}
