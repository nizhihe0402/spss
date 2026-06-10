package com.gxaysoft.project.spsscheck.expression;

import com.gxaysoft.project.spsscheck.model.RowContext;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionExpression {
    private final String condition;
    private final RowContext row;

    public ConditionExpression(String condition, RowContext row) {
        this.condition = condition;
        this.row = row;
    }

    public boolean eval() {
        // Handle MISSING(var) as a standalone condition
        String trimmed = condition.trim();
        if (ArithmeticExpression.isMissingCall(trimmed)) {
            return ArithmeticExpression.evalMissing(trimmed, row);
        }

        String[] orParts = trimmed.split("(?i)\\s+OR\\s+");
        for (String orPart : orParts) {
            if (evalOrPart(orPart.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean evalOrPart(String expression) {
        String[] andParts = expression.split("(?i)\\s+AND\\s+");
        for (String andPart : andParts) {
            if (!evalAtom(andPart.trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean evalAtom(String expression) {
        // MISSING(var) — standalone
        if (ArithmeticExpression.isMissingCall(expression)) {
            return ArithmeticExpression.evalMissing(expression, row);
        }
        // NOT MISSING(var)
        if (expression.toUpperCase().startsWith("NOT ") && ArithmeticExpression.isMissingCall(expression.substring(4))) {
            return !ArithmeticExpression.evalMissing(expression.substring(4), row);
        }

        // Try comparison: left OP right
        Matcher matcher = Pattern.compile("(.+?)(>=|<=|<>|=|>|<)(.+)", Pattern.DOTALL).matcher(expression);
        if (!matcher.matches()) {
            return false;
        }

        String leftExpr = matcher.group(1).trim();
        String op = matcher.group(2);
        String rightExpr = matcher.group(3).trim();

        // String comparison (contains " or LTRIM/RTRIM/CHAR on either side)
        if (isStringExpr(leftExpr) || isStringExpr(rightExpr) || rightExpr.startsWith("\"") || rightExpr.startsWith("'")) {
            return evalStringComparison(leftExpr, op, rightExpr);
        }

        // Numeric comparison
        BigDecimal left = new ArithmeticExpression(leftExpr, row).parse();
        BigDecimal right = new ArithmeticExpression(rightExpr, row).parse();
        if (left == null || right == null) {
            return false;
        }
        return compare(left, op, right);
    }

    private boolean isStringExpr(String expr) {
        String upper = expr.toUpperCase().trim();
        return upper.contains("LTRIM") || upper.contains("RTRIM") || upper.contains("CHAR.SUBSTR")
                || upper.contains("STRING") || upper.contains("MISSING");
    }

    private boolean evalStringComparison(String leftExpr, String op, String rightExpr) {
        String left = ArithmeticExpression.evalString(leftExpr, row);
        String right = ArithmeticExpression.evalString(rightExpr, row);

        if (left == null && right == null) return "=".equals(op);
        if (left == null || right == null) return "<>".equals(op);

        switch (op) {
            case "=":  return left.equals(right);
            case "<>": return !left.equals(right);
            default:   return false;
        }
    }

    private boolean compare(BigDecimal left, String op, BigDecimal right) {
        int cmp = left.compareTo(right);
        switch (op) {
            case ">=": return cmp >= 0;
            case "<=": return cmp <= 0;
            case ">":  return cmp > 0;
            case "<":  return cmp < 0;
            case "=":  return cmp == 0;
            case "<>": return cmp != 0;
            default:   return false;
        }
    }
}
