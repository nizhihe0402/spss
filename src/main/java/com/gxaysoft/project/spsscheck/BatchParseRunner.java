package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.persistence.*;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.*;

/**
 * 批量解析目录下所有 .sps 文件（含子目录），输出解析摘要。
 * 加 {@code --save} 时写入 MySQL（需本地 MySQL 运行）。
 *
 * <pre>
 * java -Dfile.encoding=UTF-8 -cp target/classes com.gxaysoft.project.spsscheck.BatchParseRunner
 * java ... BatchParseRunner --save                                       # 解析并入库
 * java ... BatchParseRunner path/to/sps --save                           # 指定目录+入库
 * </pre>
 */
public class BatchParseRunner {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        boolean save = false;
        Path dir = null;
        for (String arg : args) {
            if ("--save".equals(arg)) save = true;
            else dir = Paths.get(arg);
        }
        if (dir == null) dir = Paths.get("docs/sources/sps");
        if (!Files.isDirectory(dir)) {
            System.err.println("目录不存在: " + dir);
            System.exit(1);
        }

        List<Path> spsFiles = new ArrayList<>();
        Files.walk(dir).filter(p -> p.toString().endsWith(".sps")).forEach(spsFiles::add);
        spsFiles.sort(Comparator.naturalOrder());

        Connection conn = null;
        if (save) {
            conn = DbConnection.get();
            conn.setAutoCommit(false);
            SchemaInitializer.ensureTables(conn);
            System.out.println("=== 保存模式：已连接 MySQL ===");
        }

        int totalRules = 0, totalDataset = 0, totalOutput = 0;
        int totalFiles = 0, totalErrors = 0;
        int inserted = 0;

        String title = save ? "%-42s %6s %6s %6s %6s %s%n" : "%-42s %6s %6s %6s %6s %s%n";
        System.out.printf(title, "文件", "规则", "Dataset", "Output", "耗时ms", "备注");
        System.out.println(repeat('-', 85));

        for (Path spsFile : spsFiles) {
            String name = dir.relativize(spsFile).toString().replace('\\', '/');
            long t0 = System.currentTimeMillis();
            try {
                String text = PrototypeFileReaders.readSpssText(spsFile);
                ParsedScript parsed = SpssParser.parse(text);

                int ruleCount = parsed.getRules().size();
                int dsCount = parsed.getDatasetRules().size();
                int outCount = parsed.getOutputRules().size();

                totalRules += ruleCount;
                totalDataset += dsCount;
                totalOutput += outCount;
                totalFiles++;

                long ms = System.currentTimeMillis() - t0;
                String note = "";
                if (save) {
                    int n = saveToDb(conn, name, text, parsed);
                    inserted += n;
                    note = n > 0 ? "入" + n : "跳过";
                }
                System.out.printf("%-42s %6d %6d %6d %5dms %s%n",
                        name, ruleCount, dsCount, outCount, ms, note);
            } catch (Exception e) {
                totalErrors++;
                long ms = System.currentTimeMillis() - t0;
                System.out.printf("%-42s %6s %6s %6s %5dms  ERROR: %s%n",
                        name, "-", "-", "-", ms, e.getMessage());
            }
        }

        System.out.println(repeat('-', 85));
        String sumNote = save ? "入库=" + inserted : "";
        System.out.printf("%-42s %6d %6d %6d  文件=%d 错误=%d %s%n",
                "合计", totalRules, totalDataset, totalOutput, totalFiles, totalErrors, sumNote);

        if (conn != null) {
            conn.commit();
            conn.close();
            System.out.println("\n已提交。");
        }
    }

    /** 将解析结果保存到数据库（per-script upsert）。 */
    private static int saveToDb(Connection conn, String scriptName, String spsText,
                                 ParsedScript parsed) throws Exception {
        SpsRepository repo = new SpsRepository(conn);
        String cleanName = scriptName.replace(".sps", "").trim();
        // 删除旧数据
        java.sql.Statement st = conn.createStatement();
        st.execute("DELETE FROM sps_rule_step WHERE rule_id IN " +
                "(SELECT id FROM sps_rule WHERE script_id IN " +
                "(SELECT id FROM sps_script WHERE script_name='" +
                cleanName.replace("'", "''") + "'))");
        st.execute("DELETE FROM sps_rule WHERE script_id IN " +
                "(SELECT id FROM sps_script WHERE script_name='" +
                cleanName.replace("'", "''") + "')");
        st.execute("DELETE FROM sps_unsupported_statement WHERE script_id IN " +
                "(SELECT id FROM sps_script WHERE script_name='" +
                cleanName.replace("'", "''") + "')");
        st.execute("DELETE FROM sps_script WHERE script_name='" +
                cleanName.replace("'", "''") + "'");
        st.close();

        // 推断 table_id：结局判定/xxx → xxx
        String lookup = cleanName.contains("/") ? cleanName.substring(cleanName.lastIndexOf('/') + 1) : cleanName;
        long tableId = ScriptQuestionMappingService.inferTableIdFromScriptName(lookup);

        long scriptId = repo.insertScript(cleanName, spsText, cleanName, tableId);
        if (tableId > 0) {
            repo.insertScriptQuestionMappings(scriptId,
                    ScriptQuestionMappingService.loadQuestionMappings(conn, tableId));
        }

        int sortNo = 0;
        for (Rule rule : parsed.getRules()) {
            sortNo++;
            repo.insertRule(scriptId, sortNo, rule);
        }
        for (String[] stmt : SpsRepository.collectUnsupported(spsText)) {
            repo.insertUnsupportedStatement(scriptId, stmt[0], stmt[1], stmt[2]);
        }
        return sortNo;
    }

    private static String repeat(char c, int n) {
        char[] arr = new char[n];
        Arrays.fill(arr, c);
        return new String(arr);
    }
}
