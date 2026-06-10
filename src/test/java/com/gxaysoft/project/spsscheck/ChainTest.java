package com.gxaysoft.project.spsscheck;

import com.gxaysoft.project.spsscheck.v1.executor.*;
import com.gxaysoft.project.spsscheck.io.*;
import com.gxaysoft.project.spsscheck.model.*;
import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.v1.parser.*;
import com.gxaysoft.project.spsscheck.parser.*;
import java.nio.file.*;
import java.util.*;

/**
 * Tests multi-stage pipeline: Stage1 = cleaning, Stage2 = outcome determination.
 */
public class ChainTest {
    public static void main(String[] args) throws Exception {
        Path jsonPath = Paths.get("docs/sources/sql/bus_question_202606081429.json");
        Path csvPath = Paths.get("docs/sources/cvs/表2-1/bus_doctor_answer_202606081108.csv");
        Path studentPath = Paths.get("docs/sources/data/学生证件类型证件号.json");

        List<AnswerRecord> answers = PrototypeFileReaders.readAnswerCsv(csvPath);
        long tableId = TableIdDetector.detectMostFrequentTableId(answers);
        Map<String, QuestionMapping> mappings = QuestionJsonParser.parseQuestionMappings(jsonPath, tableId);

        StudentInfoLoader.LoadResult studentData = null;
        if (Files.exists(studentPath)) {
            studentData = StudentInfoLoader.load(studentPath);
            mappings.putAll(studentData.mappings);
        }

        // ==== Stage 1: table 2-1 cleaning ====
        List<RowContext> rows = AnswerPivot.pivot(answers, mappings);
        if (studentData != null) StudentInfoLoader.enrichRows(rows, studentData.studentInfo);

        String sps21 = PrototypeFileReaders.readSpssText(Paths.get("docs/sources/sps/表2-1.sps"));
        List<SpssCheckRule> rules21 = SpssRuleParser.parseRules(sps21);
        List<SpssDatasetRule> ds21 = SpssRuleParser.parseDatasetRules(sps21);
        RuleEngine.execute(rows, rules21);
        for (SpssDatasetRule dr : ds21) dr.execute(rows);
        System.out.printf("Stage1 (table 2-1): %d rules on %d rows%n", rules21.size(), rows.size());

        // Add all computed variables to mappings so checker knows they exist
        for (SpssCheckRule r : rules21) {
            String target = r.getTarget();
            if (!mappings.containsKey(target)) {
                mappings.put(target, new QuestionMapping(-1, target, target, -1));
            }
        }

        // ==== Stage 2: outcome determination ====
        String[] scripts = {
                "docs/sources/sps/结局判定/超重肥胖生长迟缓营养不良语法.sps",
                "docs/sources/sps/结局判定/血压偏高语法.sps",
                "docs/sources/sps/结局判定/脊柱弯曲判定-1126.sps"
        };

        for (String path : scripts) {
            String name = Paths.get(path).getFileName().toString().replace(".sps", "");
            String spsText = PrototypeFileReaders.readSpssText(Paths.get(path));
            List<SpssCheckRule> rules = SpssRuleParser.parseRules(spsText);
            List<SpssDatasetRule> ds = SpssRuleParser.parseDatasetRules(spsText);

            List<RuleAvailability> avail = RuleAvailabilityChecker.check(rules, ds, mappings);
            long exec = avail.stream().filter(RuleAvailability::isExecutable).count();
            int failed = rules.size() - (int) exec;

            // Debug: print all parsed rules and their sources
            System.out.printf("%n--- %s: %d rules ---%n", name, rules.size());
            for (SpssCheckRule r : rules) {
                boolean ok = true;
                for (RuleAvailability a : avail) {
                    if (a.getRule() == r) { ok = a.isExecutable(); break; }
                }
                System.out.printf("  [%s] %-20s sources=%-30s steps=%d%n",
                    ok ? "OK" : "XX", r.getTarget(), r.getSourceVariables(), r.getSteps().size());
            }

            if (failed > 0) {
                System.out.printf("Stage2 (%-20s): %d/%d executable, failed:%n", name, exec, rules.size());
                for (RuleAvailability a : avail) {
                    if (!a.isExecutable())
                        System.out.printf("  %s missing=%s%n", a.getRule().getTarget(), a.getMissingVariables());
                }
            } else {
                System.out.printf("Stage2 (%-20s): %d/%d FULL%n", name, exec, rules.size());
            }
        }
    }
}
