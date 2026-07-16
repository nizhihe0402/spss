package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import com.gxaysoft.project.spsscheck.engine.parser.ParsedScript;
import com.gxaysoft.project.spsscheck.engine.parser.SpssParser;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 批量解析目录下所有 .sps 文件，输出每个文件的解析摘要。
 *
 * <pre>
 * java -Dfile.encoding=UTF-8 -cp target/classes com.gxaysoft.project.spsscheck.BatchParseRunner [sps-dir]
 * </pre>
 */
public class BatchParseRunner {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        Path dir = args.length > 0 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");
        if (!Files.isDirectory(dir)) {
            System.err.println("目录不存在: " + dir);
            System.exit(1);
        }

        List<Path> spsFiles = new ArrayList<>();
        Files.walk(dir).filter(p -> p.toString().endsWith(".sps")).forEach(spsFiles::add);
        spsFiles.sort(Comparator.naturalOrder());

        int totalRules = 0, totalDataset = 0, totalOutput = 0;
        int totalFiles = 0, totalErrors = 0;

        System.out.println(String.format("%-42s %6s %6s %6s %6s %s",
                "文件", "规则", "Dataset", "Output", "耗时ms", "备注"));
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
                System.out.println(String.format("%-42s %6d %6d %6d %5dms",
                        name, ruleCount, dsCount, outCount, ms));
            } catch (Exception e) {
                totalErrors++;
                long ms = System.currentTimeMillis() - t0;
                System.out.println(String.format("%-42s %6s %6s %6s %5dms  ERROR: %s",
                        name, "-", "-", "-", ms, e.getMessage()));
            }
        }

        System.out.println(repeat('-', 85));
        System.out.println(String.format("%-42s %6d %6d %6d  文件=%d 错误=%d",
                "合计", totalRules, totalDataset, totalOutput, totalFiles, totalErrors));
    }

    private static String repeat(char c, int n) {
        char[] arr = new char[n];
        Arrays.fill(arr, c);
        return new String(arr);
    }
}
