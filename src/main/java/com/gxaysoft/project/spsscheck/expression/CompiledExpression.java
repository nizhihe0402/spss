package com.gxaysoft.project.spsscheck.expression;

import com.gxaysoft.project.spsscheck.model.RowContext;
import java.math.BigDecimal;

public class CompiledExpression {
    private final String expression;

    CompiledExpression(String expression) {
        this.expression = expression;
    }

    public BigDecimal evaluate(RowContext row) {
        return new ArithmeticExpression(expression, row).parse();
    }

    public String getExpression() { return expression; }
}
