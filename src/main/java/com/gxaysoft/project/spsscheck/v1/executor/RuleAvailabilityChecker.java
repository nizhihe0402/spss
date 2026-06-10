package com.gxaysoft.project.spsscheck.v1.executor;

import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class RuleAvailabilityChecker {
    private RuleAvailabilityChecker() {
    }

    public static List<RuleAvailability> check(List<SpssCheckRule> rules,
                                               List<SpssDatasetRule> datasetRules,
                                               Map<String, QuestionMapping> questionMappings) {
        LinkedHashSet<String> available = new LinkedHashSet<>();
        for (String variable : questionMappings.keySet()) {
            available.add(SpssUtil.normalize(variable));
        }
        for (SpssDatasetRule datasetRule : datasetRules) {
            available.add(SpssUtil.normalize(datasetRule.getFirstVariable()));
            available.add(SpssUtil.normalize(datasetRule.getLastVariable()));
        }

        List<RuleAvailability> result = new ArrayList<>();
        for (SpssCheckRule rule : rules) {
            List<String> missing = new ArrayList<>();
            for (String sourceVariable : rule.getSourceVariables()) {
                String normalized = SpssUtil.normalize(sourceVariable);
                if (!available.contains(normalized)) {
                    missing.add(sourceVariable);
                }
            }
            boolean executable = missing.isEmpty();
            result.add(new RuleAvailability(rule, executable, missing));
            if (executable) {
                available.add(SpssUtil.normalize(rule.getTarget()));
            }
        }

        // Second pass: rules that were blocked by ordering may now have their deps available
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < result.size(); i++) {
                RuleAvailability ra = result.get(i);
                if (ra.isExecutable()) continue;
                List<String> missing = new ArrayList<>();
                for (String sourceVariable : ra.getRule().getSourceVariables()) {
                    String normalized = SpssUtil.normalize(sourceVariable);
                    if (!available.contains(normalized)) {
                        missing.add(sourceVariable);
                    }
                }
                if (missing.isEmpty()) {
                    result.set(i, new RuleAvailability(ra.getRule(), true, java.util.Collections.<String>emptyList()));
                    available.add(SpssUtil.normalize(ra.getRule().getTarget()));
                    changed = true;
                }
            }
        }

        return result;
    }
}
