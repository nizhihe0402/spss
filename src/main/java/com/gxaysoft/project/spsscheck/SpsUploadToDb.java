package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.persistence.*;

import java.nio.file.*;
import java.sql.Connection;
import java.util.List;

/**
 * Recursively reads all .sps files under a directory, parses them,
 * and saves rules to MySQL database.  Per-script upsert — re-running
 * is safe (deletes old rules for the same script name before inserting).
 *
 * <pre>
 * java -cp target/classes:mysql-connector.jar \\
 *   com.gxaysoft.project.spsscheck.SpsUploadToDb [sps-dir]
 * </pre>
 */
public class SpsUploadToDb {
    public static void main(String[] args) throws Exception {
        Path spsDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");

        System.out.println("=== SPS Upload to Database ===");
        System.out.println("SPS dir: " + spsDir.toAbsolutePath());
        System.out.println("DB    : localhost:3306/h2025");
        System.out.println();

        try (Connection conn = DbConnection.get()) {
            conn.setAutoCommit(false);
            SchemaInitializer.ensureTables(conn);
            System.out.println("Tables verified/created.\n");

            SpsRepository repo = new SpsRepository(conn);

            List<Path> spsFiles = Files.walk(spsDir)
                    .filter(p -> p.toString().endsWith(".sps"))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            int totalRules = 0, totalFiles = 0;

            for (Path spsFile : spsFiles) {
                String name = spsDir.relativize(spsFile).toString().replace('\\', '/');
                String spsName = name.replace(".sps", "");
                System.out.println("--- " + spsName + " ---");

                // Per-script upsert: delete old data for this script name
                java.sql.Statement st = conn.createStatement();
                st.execute("DELETE FROM sps_rule_step WHERE rule_id IN " +
                        "(SELECT id FROM sps_rule WHERE script_id IN " +
                        "(SELECT id FROM sps_script WHERE script_name='" +
                        spsName.replace("'", "''") + "'))");
                st.execute("DELETE FROM sps_rule WHERE script_id IN " +
                        "(SELECT id FROM sps_script WHERE script_name='" +
                        spsName.replace("'", "''") + "')");
                st.execute("DELETE FROM sps_unsupported_statement WHERE script_id IN " +
                        "(SELECT id FROM sps_script WHERE script_name='" +
                        spsName.replace("'", "''") + "')");
                st.execute("DELETE FROM sps_script WHERE script_name='" +
                        spsName.replace("'", "''") + "'");
                st.close();

                String spsText = PrototypeFileReaders.readSpssText(spsFile);
                ParsedScript parsed = SpssParser.parse(spsText);
                List<Rule> rules = parsed.getRules();
                List<DatasetRule> datasetRules = parsed.getDatasetRules();

                // Infer table_id from filename (strip subdirectory prefix)
                String lookup = spsName.contains("/")
                        ? spsName.substring(spsName.lastIndexOf('/') + 1) : spsName;
                long tableId = ScriptQuestionMappingService.inferTableIdFromScriptName(lookup);

                long scriptId = repo.insertScript(spsName, spsText, spsName, tableId);
                if (tableId > 0) {
                    repo.insertScriptQuestionMappings(scriptId,
                            ScriptQuestionMappingService.loadQuestionMappings(conn, tableId));
                }
                System.out.printf("  script_id=%d, %d rules, %d dataset rules%n",
                        scriptId, rules.size(), datasetRules.size());

                int sortNo = 0;
                for (Rule rule : rules) {
                    sortNo++;
                    repo.insertRule(scriptId, sortNo, rule);
                }
                for (DatasetRule dr : datasetRules) {
                    sortNo++;
                    Rule wrapper = new Rule(dr.getFirstVariable(), RuleType.DUPLICATE_MARK,
                            dr.getSpssSource(),
                            java.util.Collections.singletonList(dr.getByVariable()));
                    wrapper.setCheckRule(true);
                    wrapper.setJavaPreview(dr.getJavaRule());
                    repo.insertRule(scriptId, sortNo, wrapper);
                }
                for (String[] stmt : SpsRepository.collectUnsupported(spsText)) {
                    repo.insertUnsupportedStatement(scriptId, stmt[0], stmt[1], stmt[2]);
                }

                totalRules += sortNo;
                totalFiles++;
            }

            conn.commit();
            System.out.println("\n=== COMMIT OK: " + totalFiles + " files, " + totalRules + " rules ===");
        }
    }
}
