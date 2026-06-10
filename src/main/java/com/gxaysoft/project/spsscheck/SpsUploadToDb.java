package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;
import com.gxaysoft.project.spsscheck.persistence.*;

import java.nio.file.*;
import java.sql.Connection;
import java.util.List;

/**
 * Reads all .sps files, parses them, and saves rules to MySQL database.
 *
 * Usage: java ... SpsUploadToDb [sps-dir]
 */
public class SpsUploadToDb {
    public static void main(String[] args) throws Exception {
        Path spsDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");

        System.out.println("=== SPS Upload to Database ===");
        System.out.println("SPS dir: " + spsDir.toAbsolutePath());
        System.out.println("DB    : localhost:3306/healthdetection_2025");
        System.out.println();

        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);

            // Ensure tables exist
            SchemaInitializer.ensureTables(conn);
            System.out.println("Tables verified/created.");

            // Clean previous data for re-upload
            conn.createStatement().execute("DELETE FROM sps_unsupported_statement");
            conn.createStatement().execute("DELETE FROM sps_rule_step");
            conn.createStatement().execute("DELETE FROM sps_output_rule");
            conn.createStatement().execute("DELETE FROM sps_rule");
            conn.createStatement().execute("DELETE FROM sps_script");
            System.out.println("Previous data cleaned.");

            SpsRepository repo = new SpsRepository(conn);

            List<Path> spsFiles = Files.list(spsDir)
                    .filter(p -> p.toString().endsWith(".sps") && !p.getFileName().toString().startsWith("修"))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            for (Path spsFile : spsFiles) {
                String spsName = spsFile.getFileName().toString().replace(".sps", "");
                System.out.println("--- " + spsName + " ---");

                String spsText = PrototypeFileReaders.readSpssText(spsFile);

                // Parse
                List<SpssCheckRule> rules = SpssRuleParser.parseRules(spsText);
                List<SpssDatasetRule> datasetRules = SpssRuleParser.parseDatasetRules(spsText);
                List<SpssOutputRule> outputRules = SpssRuleParser.parseOutputRules(spsText);

                // Insert script
                long scriptId = repo.insertScript(spsName, spsText, spsName, -1);
                System.out.printf("  script_id=%d, %d rules, %d dataset rules, %d output rules%n",
                        scriptId, rules.size(), datasetRules.size(), outputRules.size());

                // Insert check rules + steps
                int sortNo = 0;
                for (SpssCheckRule rule : rules) {
                    sortNo++;
                    repo.insertRule(scriptId, sortNo, rule);
                }

                // Insert dataset rules as special ROW_CHECK rules
                for (SpssDatasetRule dr : datasetRules) {
                    sortNo++;
                    String target = dr.getFirstVariable();
                    SpssCheckRule wrapper = new SpssCheckRule(target, "",
                            "去重标记", java.util.Collections.singletonList(dr.getByVariable()),
                            true, dr.getSpssSource(), dr.getJavaRule());
                    repo.insertRule(scriptId, sortNo, wrapper);
                }

                // Insert output rules
                int outNo = 0;
                for (SpssOutputRule or : outputRules) {
                    outNo++;
                    repo.insertOutputRule(scriptId, outNo, or);
                }

                // Insert unsupported statements
                for (String[] stmt : SpsRepository.collectUnsupported(spsText)) {
                    repo.insertUnsupportedStatement(scriptId, stmt[0], stmt[1], stmt[2]);
                }
            }

            conn.commit();
            System.out.println();
            System.out.println("=== COMMIT OK ===");
        }
    }
}
