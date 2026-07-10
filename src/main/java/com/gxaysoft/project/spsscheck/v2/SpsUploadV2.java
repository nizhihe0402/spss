package com.gxaysoft.project.spsscheck.v2;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;
import com.gxaysoft.project.spsscheck.persistence.*;
import com.gxaysoft.project.spsscheck.v2.model.*;
import com.gxaysoft.project.spsscheck.v2.parser.BlockParser;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * V2: Uploads SPS files to database using BlockParser + RuleDefinition.
 *
 * Usage: java ... SpsUploadV2 [sps-dir]
 */
public class SpsUploadV2 {
    public static void main(String[] args) throws Exception {
        Path spsDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");

        System.out.println("=== SPS Upload V2 (Block-based Engine) ===");

        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            SchemaInitializer.ensureTables(conn);

            // Clean previous
            conn.createStatement().execute("DELETE FROM sps_unsupported_statement");
            conn.createStatement().execute("DELETE FROM sps_rule_step");
            conn.createStatement().execute("DELETE FROM sps_output_rule");
            conn.createStatement().execute("DELETE FROM sps_rule");
            conn.createStatement().execute("DELETE FROM sps_script");
            System.out.println("Previous data cleaned.");

            List<Path> spsFiles = Files.walk(spsDir)
                    .filter(p -> p.toString().endsWith(".sps") && !p.getFileName().toString().startsWith("修"))
                    .sorted().collect(java.util.stream.Collectors.toList());

            for (Path spsFile : spsFiles) {
                String name = spsFile.getFileName().toString().replace(".sps", "");
                String tableCode = name;
                Path rel = spsDir.relativize(spsFile);
                if (rel.getParent() != null) name = rel.getParent().toString().replace("\\", "/") + "/" + name;
                System.out.println("--- " + name + " ---");
                String spsText = PrototypeFileReaders.readSpssText(spsFile);

                // V2: Block-based parsing
                List<RuleDefinition> rules = BlockParser.parse(spsText);
                List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);

                // Insert script
                long scriptId = insertScript(conn, name, spsText);
                System.out.printf("  script_id=%d, %d blocks, %d output rules%n",
                        scriptId, rules.size(), outputRules.size());

                // Insert rules
                int sortNo = 0;
                for (RuleDefinition rd : rules) {
                    sortNo++;
                    insertRuleV2(conn, scriptId, sortNo, rd);
                }

                // Insert output rules
                int outNo = 0;
                for (SpssOutputRule or : outputRules) {
                    outNo++;
                    insertOutputRule(conn, scriptId, outNo, or);
                }

                // Unsupported statements
                for (String[] stmt : SpsRepository.collectUnsupported(spsText)) {
                    conn.createStatement().execute(
                        "INSERT INTO sps_unsupported_statement (script_id,statement_type,reason,risk_level) VALUES (" +
                        scriptId + ",'" + stmt[0] + "','" + stmt[1] + "','" + stmt[2] + "')");
                }
            }

            conn.commit();
            System.out.println("\n=== COMMIT OK ===");
        }
    }

    static long insertScript(Connection conn, String name, String spsText) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sps_script (script_name,script_content,table_code,parse_status,version_no,status) " +
                "VALUES (?,?,?,'PARSED',1,'DRAFT')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, spsText);
            ps.setString(3, name);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getLong(1);
        }
    }

    static void insertRuleV2(Connection conn, long scriptId, int sortNo, RuleDefinition rd) throws SQLException {
        String code = String.format("R%03d", sortNo);
        String sources = String.join(",", rd.getSourceVariables());
        RuleCorrectionPlan correction = RuleCorrectionPlan.detect(
                rd.getType().name(), rd.getTarget(), sources, rd.getDescription());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sps_rule (script_id,rule_code,rule_name,rule_type,target_variable," +
                "source_variables,correction_enabled,correction_type,correction_variables,correction_source," +
                "correction_strategy,correction_apply_stage,correction_write_clean,correction_write_source," +
                "correction_description,spss_source,rule_json,java_preview,warning_message,sort_no," +
                "start_line,end_line,line_count,segment_title,split_reason,affect_clean) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, scriptId);
            ps.setString(2, code);
            ps.setString(3, rd.getTarget());
            ps.setString(4, rd.getType().name());
            ps.setString(5, rd.getTarget());
            ps.setString(6, sources);
            ps.setInt(7, correction.enabled ? 1 : 0);
            ps.setString(8, correction.type);
            ps.setString(9, correction.variables);
            ps.setString(10, correction.source);
            ps.setString(11, correction.strategy);
            ps.setString(12, correction.applyStage);
            ps.setInt(13, correction.writeClean ? 1 : 0);
            ps.setInt(14, correction.writeSource ? 1 : 0);
            ps.setString(15, correction.description);
            ps.setString(16, truncate(rd.getSpssBlock(), 65535));
            ps.setString(17, "{\"v2\":true,\"type\":\"" + rd.getType().name() + "\","
                    + "\"startLine\":" + rd.getStartLine() + ","
                    + "\"endLine\":" + rd.getEndLine() + ","
                    + "\"segmentIndex\":" + rd.getSegmentIndex() + "}");
            ps.setString(18, rd.getJavaPreview());
            ps.setString(19, rd.getDescription());
            ps.setInt(20, sortNo);
            ps.setInt(21, rd.getStartLine());
            ps.setInt(22, rd.getEndLine());
            ps.setInt(23, rd.getLineCount());
            ps.setString(24, rd.getSegmentTitle());
            ps.setString(25, rd.getSplitReason());
            ps.setInt(26, rd.getType() == RuleType.IDENTITY_CHECK
                    || rd.getType() == RuleType.MISSING_CHECK
                    || rd.getType() == RuleType.RANGE_CHECK
                    || rd.getType() == RuleType.CONSISTENCY_CHECK
                    || rd.getType() == RuleType.DOCUMENT_CHECK ? 1 : 0);
            ps.executeUpdate();
        }
    }

    static void insertOutputRule(Connection conn, long scriptId, int sortNo, SpssOutputRule or) throws SQLException {
        String code = String.format("O%03d", sortNo);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sps_output_rule (script_id,output_code,output_name,output_type,select_condition," +
                "spss_source,java_preview,sort_no) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, scriptId);
            ps.setString(2, code);
            ps.setString(3, or.getSheetName());
            ps.setString(4, or.getSheetName().contains("清理后") ? "CLEAN_DATA" : "ERROR_GROUP");
            ps.setString(5, or.getCondition());
            ps.setString(6, or.getSpssSource());
            ps.setString(7, or.getJavaRule());
            ps.setInt(8, sortNo);
            ps.executeUpdate();
        }
    }

    static String truncate(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) : s;
    }
}
