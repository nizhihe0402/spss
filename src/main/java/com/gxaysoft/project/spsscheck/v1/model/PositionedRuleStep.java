package com.gxaysoft.project.spsscheck.v1.model;

public class PositionedRuleStep {
    public final int position;
    public final RuleStep step;

    public PositionedRuleStep(int position, RuleStep step) {
        this.position = position;
        this.step = step;
    }
}
