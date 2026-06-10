package com.gxaysoft.project.spsscheck.v1.model;
import com.gxaysoft.project.spsscheck.model.RowContext;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;

import java.util.List;

public class IfAssignRuleStep implements RuleStep {
    private final String condition;
    private final String target;
    private final String value;

    public IfAssignRuleStep(String condition, String target, String value) {
        this.condition = condition;
        this.target = target;
        this.value = value;
    }

    @Override
    public void execute(RowContext row) {
        if (new ConditionExpression(condition, row).eval()) {
            row.put(target, value);
        }
    }

    @Override
    public List<String> sourceVariables() {
        return SpssRuleParser.extractVariables(condition);
    }

    @Override
    public String javaRule() {
        return "if evalCondition(\"" + condition.replace("\\", "\\\\").replace("\"", "\\\"") + "\") then " + target + " = " + value;
    }
}
