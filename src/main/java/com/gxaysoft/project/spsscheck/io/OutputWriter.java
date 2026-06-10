package com.gxaysoft.project.spsscheck.io;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v1.model.SpssOutputRule;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class OutputWriter {
    private OutputWriter() {
    }

    public static void writeOutputs(Path outputDir, List<RowContext> rows, List<SpssOutputRule> outputRules) throws IOException {
        Files.createDirectories(outputDir);

        // Sort: ERROR_GROUP before CLEAN_DATA (clean output depends on all error flags)
        List<SpssOutputRule> sorted = new ArrayList<>(outputRules);
        Collections.sort(sorted, new Comparator<SpssOutputRule>() {
            public int compare(SpssOutputRule a, SpssOutputRule b) {
                boolean aClean = a.getSheetName().contains("清理后");
                boolean bClean = b.getSheetName().contains("清理后");
                if (aClean == bClean) return 0;
                return aClean ? 1 : -1; // non-清理后 first
            }
        });

        for (SpssOutputRule rule : sorted) {
            List<RowContext> matched = new ArrayList<>();
            for (RowContext row : rows) {
                if (rule.matches(row)) {
                    matched.add(row);
                }
            }
            if (matched.isEmpty()) {
                continue;
            }

            String fileName = sanitizeFileName(rule.getSheetName()) + ".csv";
            Path filePath = outputDir.resolve(fileName);

            // Collect all variable names across matched rows
            LinkedHashSet<String> allVars = new LinkedHashSet<>();
            for (RowContext row : matched) {
                allVars.addAll(row.getValues().keySet());
            }

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath, StandardCharsets.UTF_8))) {
                // BOM for Excel UTF-8 detection
                writer.print('﻿');

                // Header
                writer.print("sample_key");
                for (String var : allVars) {
                    writer.print(',');
                    writer.print(escapeCsv(var));
                }
                writer.println();

                // Data rows
                for (RowContext row : matched) {
                    writer.print(escapeCsv(row.getSampleKey()));
                    Map<String, Object> values = row.getValues();
                    for (String var : allVars) {
                        writer.print(',');
                        Object val = values.get(var);
                        writer.print(val == null ? "" : escapeCsv(String.valueOf(val)));
                    }
                    writer.println();
                }
            }

            System.out.println("  wrote " + matched.size() + " rows -> " + filePath);
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").replace('\t', '_');
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
