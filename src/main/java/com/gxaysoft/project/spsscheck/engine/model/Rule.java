package com.gxaysoft.project.spsscheck.engine.model;

import java.util.ArrayList;
import java.util.List;

public class Rule {
    private String target;
    private RuleType type;
    private String description;
    private List<Step> steps;
    private String expression;
    private boolean checkRule;
    private String spssSource;
    private SegmentInfo segment;
    private String javaPreview;
    private String executionChain;
    private List<String> sourceVariables;

    public Rule() {
        this.steps = new ArrayList<>();
        this.sourceVariables = new ArrayList<>();
        this.type = RuleType.COMPUTE_INTERMEDIATE;
    }

    public Rule(String target, RuleType type, String spssSource, List<String> sourceVariables) {
        this();
        this.target = target;
        this.type = type;
        this.spssSource = spssSource;
        this.sourceVariables = sourceVariables != null ? new ArrayList<>(sourceVariables) : new ArrayList<>();
    }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps != null ? steps : new ArrayList<>(); }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public boolean isCheckRule() { return checkRule; }
    public void setCheckRule(boolean checkRule) { this.checkRule = checkRule; }
    public String getSpssSource() { return spssSource; }
    public void setSpssSource(String spssSource) { this.spssSource = spssSource; }
    public SegmentInfo getSegment() { return segment; }
    public void setSegment(SegmentInfo segment) { this.segment = segment; }
    public String getJavaPreview() { return javaPreview; }
    public void setJavaPreview(String javaPreview) { this.javaPreview = javaPreview; }
    public String getExecutionChain() { return executionChain; }
    public void setExecutionChain(String executionChain) { this.executionChain = executionChain; }
    public List<String> getSourceVariables() { return sourceVariables; }
    public void setSourceVariables(List<String> sourceVariables) { this.sourceVariables = sourceVariables != null ? sourceVariables : new ArrayList<>(); }

    public void addStep(Step step) { if (step != null) this.steps.add(step); }

    public int getStartLine() { return segment != null ? segment.getStartLine() : 0; }
    public int getEndLine() { return segment != null ? segment.getEndLine() : 0; }
    public String getSegmentTitle() { return segment != null ? segment.getSegmentTitle() : null; }
    public void setStartLine(int line) { if (segment == null) segment = new SegmentInfo(); segment.setStartLine(line); }
    public void setEndLine(int line) { if (segment == null) segment = new SegmentInfo(); segment.setEndLine(line); }

    public String toString() { return "Rule{target='" + target + "', type=" + type + ", steps=" + steps.size() + "}"; }
}
