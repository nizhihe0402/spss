package com.gxaysoft.project.spsscheck.v2;

import com.gxaysoft.project.spsscheck.v2.executor.BlockExecutor;
import com.gxaysoft.project.spsscheck.v2.model.RuleDefinition;
import com.gxaysoft.project.spsscheck.v2.model.RuleType;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * V2 engine runner: scans all .sps files, parses with SpsBlockParser, runs with handlers.
 *
 * Usage: java ... SpsRunV2 [sps-dir] [mapping-json] [csv-data-dir]
 */
public class V2Runner {
    public static void main(String[] args) throws Exception {
        Path spsDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get("docs/sources/sps");
        Path mapping = args.length >= 2 ? Paths.get(args[1]) : Paths.get("docs/sources/sql/bus_question_202606081429.json");
        Path csvDir = args.length >= 3 ? Paths.get(args[2]) : Paths.get("docs/sources/cvs");
        Path student = Paths.get("docs/sources/data/学生证件类型证件号.json");

        List<Path> spsFiles = Files.list(spsDir)
                .filter(p -> p.toString().endsWith(".sps") && !p.getFileName().toString().startsWith("修"))
                .sorted().collect(Collectors.toList());

        // Summary
        System.out.println("=== SPS Run V2 (Handler-based Engine) ===");
        Map<RuleType, Integer> globalCounts = new LinkedHashMap<>();
        int totalRules = 0;

        for (Path spsFile : spsFiles) {
            BlockExecutor.RunResult r = BlockExecutor.run(spsFile, mapping, findCsv(csvDir, spsFile), student);
            BlockExecutor.printReport(r);

            totalRules += r.rules.size();
            for (Map.Entry<RuleType, Integer> e : r.typeCounts.entrySet()) {
                globalCounts.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }

        System.out.println("\n═══════════════════════════════════");
        System.out.println("Total: " + totalRules + " rules across " + spsFiles.size() + " tables");
        System.out.println("By type:");
        for (Map.Entry<RuleType, Integer> e : globalCounts.entrySet()) {
            System.out.printf("  %-25s %d%n", e.getKey().label, e.getValue());
        }
    }

    private static Path findCsv(Path csvDir, Path spsFile) throws Exception {
        String name = spsFile.getFileName().toString().replace(".sps", "");
        Path dir = csvDir.resolve(name);
        if (!Files.isDirectory(dir)) return null;
        return Files.list(dir)
                .filter(p -> p.toString().endsWith(".csv") && !p.toString().contains("intervene"))
                .findFirst().orElse(null);
    }
}
