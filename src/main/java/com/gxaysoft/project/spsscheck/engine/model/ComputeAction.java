package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import java.math.BigDecimal;
import java.util.*;

public class ComputeAction implements StepAction {
    private final String target;
    private final String expression;

    public ComputeAction(String target, String expression) {
        this.target = target;
        this.expression = expression;
    }

    @Override
    public void execute(RowContext row) {
        // Note: uses reflection-like approach via ArithmeticExpression for now
        // AST pre-compilation will be added in Task 2.2
        row.put(target, new com.gxaysoft.project.spsscheck.expression.ArithmeticExpression(expression, row).parse());
    }

    @Override
    public String target() { return target; }

    public String getExpression() { return expression; }

    @Override
    public List<String> sourceVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        List<String> raw = SpssUtil.extractVariables(expression);
        for (String v : raw) {
            String norm = SpssUtil.normalize(v);
            if (!norm.equals(SpssUtil.normalize(target))) {
                vars.put(norm, v.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(vars.values());
    }
}
