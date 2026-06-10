package com.gxaysoft.project.spsscheck.v2.handler;

import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v1.model.RuleStep;
import com.gxaysoft.project.spsscheck.v2.model.*;

import java.math.BigDecimal;
import java.util.List;

/** 计算中间变量：执行 COMPUTE expression 或 stored RuleSteps */
public class ComputeHandler implements RuleHandler {
    @Override public RuleType handles() { return RuleType.COMPUTE_INTERMEDIATE; }

    @Override
    public void execute(RuleDefinition rule, RowContext row) {
        // 1. Execute stored RuleSteps if available
        List<RuleStep> steps = rule.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (RuleStep step : steps) {
                try { step.execute(row); } catch (Exception ignored) {}
            }
            return;
        }
        // 2. Fallback: evaluate expression directly
        if (rule.getExpression() == null) return;
        BigDecimal value = new ArithmeticExpression(rule.getExpression(), row).parse();
        row.put(rule.getTarget(), value);
    }

    @Override
    public String describe(RuleDefinition rule) {
        return "计算" + rule.getTarget() + " = " + rule.getExpression();
    }
}
