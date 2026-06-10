package com.gxaysoft.project.spsscheck.v1.model;

import java.util.List;

public class RuleAvailability {
    private final SpssCheckRule rule;
    private final boolean executable;
    private final List<String> missingVariables;

    public RuleAvailability(SpssCheckRule rule, boolean executable, List<String> missingVariables) {
        this.rule = rule;
        this.executable = executable;
        this.missingVariables = missingVariables;
    }

    public SpssCheckRule getRule() {
        return rule;
    }

    public boolean isExecutable() {
        return executable;
    }

    public List<String> getMissingVariables() {
        return missingVariables;
    }
}
