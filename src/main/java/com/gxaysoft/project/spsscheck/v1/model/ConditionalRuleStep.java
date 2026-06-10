package com.gxaysoft.project.spsscheck.v1.model;
import com.gxaysoft.project.spsscheck.model.RowContext;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.v1.parser.SpssRuleParser;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ConditionalRuleStep implements RuleStep {
    private final String condition;
    private final RuleStep delegate;

    public ConditionalRuleStep(String condition, RuleStep delegate) {
        this.condition = condition;
        this.delegate = delegate;
    }

    @Override
    public void execute(RowContext row) {
        if (new ConditionExpression(condition, row).eval()) {
            delegate.execute(row);
        }
    }

    @Override
    public List<String> sourceVariables() {
        LinkedHashMap<String, String> vars = new LinkedHashMap<>();
        for (String variable : delegate.sourceVariables()) {
            vars.put(SpssUtil.normalize(variable), variable.toUpperCase(Locale.ROOT));
        }
        for (String variable : SpssRuleParser.extractVariables(condition)) {
            vars.put(SpssUtil.normalize(variable), variable.toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(vars.values());
    }

    @Override
    public String javaRule() {
        return "if evalCondition(\"" + condition.replace("\\", "\\\\").replace("\"", "\\\"") + "\") then { " + delegate.javaRule() + " }";
    }
}
