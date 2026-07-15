package com.gxaysoft.project.spsscheck.expression;

import com.gxaysoft.project.spsscheck.model.RowContext;

import java.math.BigDecimal;

/**
 * 条件表达式求值器（递归下降）。
 *
 * <p>文法（优先级由低到高）：
 * <pre>
 *   or    := and ( ("OR" | "|") and )*
 *   and   := not ( ("AND" | "&") not )*
 *   not   := "NOT" not | term
 *   term  := "(" or ")"      —— 当括号内顶层含比较/布尔运算符时视为布尔分组
 *          | atom            —— 比较式或 MISSING(...)
 * </pre>
 * 括号既可能是布尔分组，也可能是算术分组（如 {@code (a+b) > 5}），由
 * {@link #isBooleanGroup} 依据括号内顶层是否含比较/布尔运算符来区分。比较式左右两侧
 * 交由 {@link ArithmeticExpression} 求值；字符串比较沿用原有逻辑。修复了旧的按字符串
 * split 实现无法处理带括号布尔与一般 {@code NOT(...)}（导致 DO IF ELSE 恒 false）的问题。
 */
public class ConditionExpression {
    private final String condition;
    private final RowContext row;
    private final String s;
    private int pos;

    public ConditionExpression(String condition, RowContext row) {
        this.condition = condition;
        this.row = row;
        this.s = condition == null ? "" : condition;
    }

    public boolean eval() {
        if (s.trim().isEmpty()) {
            return false;
        }
        pos = 0;
        return parseOr();
    }

    private boolean parseOr() {
        boolean value = parseAnd();
        while (true) {
            skipWhitespace();
            if (matchKeyword("OR") || matchChar('|')) {
                boolean right = parseAnd();
                value = value || right;
            } else {
                return value;
            }
        }
    }

    private boolean parseAnd() {
        boolean value = parseNot();
        while (true) {
            skipWhitespace();
            if (matchKeyword("AND") || matchChar('&')) {
                boolean right = parseNot();
                value = value && right;
            } else {
                return value;
            }
        }
    }

    private boolean parseNot() {
        skipWhitespace();
        if (matchKeyword("NOT")) {
            return !parseNot();
        }
        return parseTerm();
    }

    private boolean parseTerm() {
        skipWhitespace();
        if (pos < s.length() && s.charAt(pos) == '(') {
            int close = matchingParen(pos);
            if (close > pos && isBooleanGroup(pos + 1, close)) {
                pos++;
                boolean value = parseOr();
                skipWhitespace();
                if (pos < s.length() && s.charAt(pos) == ')') {
                    pos++;
                }
                return value;
            }
        }
        return parseAtom();
    }

    private boolean parseAtom() {
        skipWhitespace();
        int start = pos;
        int depth = 0;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (depth == 0) {
                if (c == '&' || c == '|') {
                    break;
                }
                if ((c == ' ' || c == '\t') && keywordAt(nextNonWhitespace(pos), "AND", "OR")) {
                    break;
                }
            }
            pos++;
        }
        return evalAtom(s.substring(start, pos).trim());
    }

    private boolean evalAtom(String expr) {
        if (expr.isEmpty()) {
            return false;
        }
        if (ArithmeticExpression.isMissingCall(expr)) {
            return ArithmeticExpression.evalMissing(expr, row);
        }
        int[] op = findComparisonOp(expr);
        if (op == null) {
            return false;
        }
        String leftExpr = expr.substring(0, op[0]).trim();
        String opStr = expr.substring(op[0], op[1]);
        String rightExpr = expr.substring(op[1]).trim();

        if (isStringExpr(leftExpr) || isStringExpr(rightExpr)
                || rightExpr.startsWith("\"") || rightExpr.startsWith("'")
                || leftExpr.startsWith("\"") || leftExpr.startsWith("'")) {
            return evalStringComparison(leftExpr, opStr, rightExpr);
        }

        BigDecimal left = new ArithmeticExpression(leftExpr, row).parse();
        BigDecimal right = new ArithmeticExpression(rightExpr, row).parse();
        if (left == null || right == null) {
            return false;
        }
        return compare(left, opStr, right);
    }

    private static int[] findComparisonOp(String e) {
        int depth = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                if (c == '>' || c == '<') {
                    if (i + 1 < e.length() && e.charAt(i + 1) == '=') {
                        return new int[]{i, i + 2};
                    }
                    if (c == '<' && i + 1 < e.length() && e.charAt(i + 1) == '>') {
                        return new int[]{i, i + 2};
                    }
                    return new int[]{i, i + 1};
                }
                if (c == '=') {
                    return new int[]{i, i + 1};
                }
            }
        }
        return null;
    }

    private boolean isBooleanGroup(int innerStart, int close) {
        int depth = 0;
        for (int i = innerStart; i < close; i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                if (c == '>' || c == '<' || c == '=' || c == '&' || c == '|') {
                    return true;
                }
                if ((c == ' ' || c == '\t') && keywordAt(nextNonWhitespace(i), "AND", "OR", "NOT")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void skipWhitespace() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private boolean matchChar(char c) {
        skipWhitespace();
        if (pos < s.length() && s.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean matchKeyword(String kw) {
        skipWhitespace();
        if (keywordAt(pos, kw)) {
            pos += kw.length();
            return true;
        }
        return false;
    }

    private boolean keywordAt(int idx, String... kws) {
        for (String kw : kws) {
            if (idx + kw.length() > s.length()) {
                continue;
            }
            if (!s.regionMatches(true, idx, kw, 0, kw.length())) {
                continue;
            }
            int after = idx + kw.length();
            if (after < s.length()) {
                char a = s.charAt(after);
                if (Character.isLetterOrDigit(a) || a == '_') {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    private int nextNonWhitespace(int idx) {
        int i = idx;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private int matchingParen(int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
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
