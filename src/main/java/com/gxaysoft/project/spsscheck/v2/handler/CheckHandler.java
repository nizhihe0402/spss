package com.gxaysoft.project.spsscheck.v2.handler;

import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v1.model.RuleStep;
import com.gxaysoft.project.spsscheck.v2.model.*;

import java.math.BigDecimal;
import java.util.List;

/** Executes check rules using stored RuleSteps (parsed by V1 engine) */
public class CheckHandler implements RuleHandler {
    private final RuleType type;

    public CheckHandler(RuleType type) { this.type = type; }
    @Override public RuleType handles() { return type; }

    @Override
    public void execute(RuleDefinition rule, RowContext row) {
        List<RuleStep> steps = rule.getSteps();
        String target = rule.getTarget();

        if (steps != null && !steps.isEmpty()) {
            for (RuleStep step : steps) {
                try { step.execute(row); } catch (Exception ignored) {}
            }
            // After executing steps, mark flag based on computed value
            markFlag(row, target);
            return;
        }

        // Fallback: evaluate expression directly
        String expr = rule.getExpression();
        if (expr != null && !expr.isEmpty() && !expr.contains("→")) {
            BigDecimal v = new ArithmeticExpression(expr, row).parse();
            int flag = (v == null || v.compareTo(BigDecimal.ZERO) != 0) ? 1 : 0;
            row.put(target, flag);
            row.putFlag(target, flag);
            return;
        }

        // Fallback: evaluate check condition
        String cond = rule.getCheckCondition();
        if (cond != null && !cond.isEmpty()) {
            boolean match = new ConditionExpression(cond, row).eval();
            int flag = match ? 1 : 0;
            row.put(target, flag);
            row.putFlag(target, flag);
        }
    }

    private void markFlag(RowContext row, String target) {
        Object val = row.get(target);
        if (val instanceof BigDecimal) {
            int flag = ((BigDecimal) val).compareTo(BigDecimal.ZERO) != 0 ? 1 : 0;
            row.putFlag(target, flag);
        } else if (val instanceof Number) {
            int flag = ((Number) val).intValue() != 0 ? 1 : 0;
            row.putFlag(target, flag);
        } else if (val instanceof String) {
            int flag = "1".equals(val) ? 1 : 0;
            row.putFlag(target, flag);
        }
    }

    @Override
    public String describe(RuleDefinition rule) {
        String desc = rule.getDescription();
        if (desc != null && !desc.isEmpty() && !"null".equals(desc)) return desc;
        return "校验" + rule.getTarget() + "：标记异常为1，正常为0";
    }
}
