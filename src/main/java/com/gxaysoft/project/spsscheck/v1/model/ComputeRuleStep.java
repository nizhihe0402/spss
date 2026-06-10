package com.gxaysoft.project.spsscheck.v1.model;
import com.gxaysoft.project.spsscheck.model.RowContext;

import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;

import java.util.Collections;
import java.util.List;

public class ComputeRuleStep implements RuleStep {
    private final String target;
    private final String expression;

    public ComputeRuleStep(String target, String expression) {
        this.target = target;
        this.expression = expression;
    }

    @Override
    public void execute(RowContext row) {
        row.put(target, new ArithmeticExpression(expression, row).parse());
    }

    @Override
    public List<String> sourceVariables() {
        return SpssRuleParser.extractVariables(expression);
    }

    @Override
    public String javaRule() {
        return target + " = evalDecimal(\"" + expression.replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }

    public String getTarget() {
        return target;
    }

    public String getExpression() {
        return expression;
    }
}
