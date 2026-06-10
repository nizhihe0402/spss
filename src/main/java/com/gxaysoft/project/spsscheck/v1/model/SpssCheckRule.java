package com.gxaysoft.project.spsscheck.v1.model;

import java.util.Collections;
import java.util.List;

public class SpssCheckRule {
    private final String target;
    private final String expression;
    private final String label;
    private final List<String> sourceVariables;
    private final boolean checkRule;
    private final String spssSource;
    private final String javaRule;
    private final List<RuleStep> steps;

    public SpssCheckRule(String target, String expression, String label, List<String> sourceVariables, boolean checkRule,
                         String spssSource, String javaRule) {
        this(target, expression, label, sourceVariables, checkRule, spssSource, javaRule, Collections.<RuleStep>emptyList());
    }

    public SpssCheckRule(String target, String expression, String label, List<String> sourceVariables, boolean checkRule,
                         String spssSource, String javaRule, List<RuleStep> steps) {
        this.target = target;
        this.expression = expression;
        this.label = label == null || label.trim().isEmpty() ? target : label;
        this.sourceVariables = sourceVariables;
        this.checkRule = checkRule;
        this.spssSource = spssSource;
        this.javaRule = javaRule;
        this.steps = steps;
    }

    public String getTarget() {
        return target;
    }

    public String getExpression() {
        return expression;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getSourceVariables() {
        return sourceVariables;
    }

    public boolean isCheckRule() {
        return checkRule;
    }

    public String getSpssSource() {
        return spssSource;
    }

    public String getJavaRule() {
        return javaRule;
    }

    public List<RuleStep> getSteps() {
        return steps;
    }

    public String getDescription() {
        return com.gxaysoft.project.spsscheck.v1.parser.RuleDescriptionBuilder.build(this);
    }
}
