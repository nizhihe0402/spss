package com.gxaysoft.project.spsscheck.expression;

import com.gxaysoft.project.spsscheck.model.RowContext;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ArithmeticExpression {
    private static final SimpleDateFormat SPSS_DATE = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);

    private final String expression;
    private final RowContext row;
    private int pos;

    public ArithmeticExpression(String expression, RowContext row) {
        this.expression = expression;
        this.row = row;
    }

    public BigDecimal parse() {
        pos = 0;
        BigDecimal value = parseAddSubtract();
        skipWhitespace();
        if (pos < expression.length()) {
            return null;
        }
        return value;
    }

    private BigDecimal parseAddSubtract() {
        BigDecimal value = parseMultiplyDivide();
        while (true) {
            skipWhitespace();
            if (match('+')) {
                BigDecimal right = parseMultiplyDivide();
                value = value == null || right == null ? null : value.add(right);
            } else if (match('-')) {
                BigDecimal right = parseMultiplyDivide();
                value = value == null || right == null ? null : value.subtract(right);
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseMultiplyDivide() {
        BigDecimal value = parsePower();
        while (true) {
            skipWhitespace();
            if (peek("**")) {
                return value;
            }
            if (match('*')) {
                BigDecimal right = parsePower();
                value = value == null || right == null ? null : value.multiply(right);
            } else if (match('/')) {
                BigDecimal right = parsePower();
                value = value == null || right == null || BigDecimal.ZERO.compareTo(right) == 0
                        ? null : value.divide(right, MathContext.DECIMAL64);
            } else {
                return value;
            }
        }
    }

    private BigDecimal parsePower() {
        BigDecimal value = parseUnary();
        skipWhitespace();
        if (peek("**")) {
            pos += 2;
            BigDecimal exponent = parseUnary();
            if (value == null || exponent == null) {
                return null;
            }
            try {
                return BigDecimal.valueOf(Math.pow(value.doubleValue(), exponent.doubleValue()));
            } catch (ArithmeticException ex) {
                return null;
            }
        }
        return value;
    }

    private BigDecimal parseUnary() {
        skipWhitespace();
        if (match('+')) {
            return parseUnary();
        }
        if (match('-')) {
            BigDecimal value = parseUnary();
            return value == null ? null : value.negate();
        }
        return parsePrimary();
    }

    private BigDecimal parsePrimary() {
        skipWhitespace();
        if (match('(')) {
            BigDecimal value = parseAddSubtract();
            match(')');
            return value;
        }
        if (pos >= expression.length()) {
            return null;
        }
        char c = expression.charAt(pos);
        if (Character.isDigit(c) || c == '.') {
            return parseNumber();
        }
        if (c == '$') {
            pos++; // skip $SYSMIS → treat as null
            parseIdentifier();
            return null;
        }
        if (Character.isLetter(c) || c == '_' || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
            String identifier = parseIdentifier();
            skipWhitespace();
            if (match('(')) {
                return evalFunction(identifier);
            }
            return row.getDecimal(identifier);
        }
        return null;
    }

    // ── Function dispatch ──────────────────────────────────────────

    private BigDecimal evalFunction(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "MOD": {
                BigDecimal a = parseAddSubtract();
                match(',');
                BigDecimal b = parseAddSubtract();
                match(')');
                if (a == null || b == null || b.compareTo(BigDecimal.ZERO) == 0) return null;
                return a.remainder(b);
            }
            case "RND": {
                BigDecimal a = parseAddSubtract();
                int scale = 0;
                if (peek(",")) { match(','); BigDecimal b = parseAddSubtract(); scale = b != null ? b.intValue() : 0; }
                match(')');
                if (a == null) return null;
                return a.setScale(Math.abs(scale), RoundingMode.HALF_UP);
            }
            case "DATEDIFF":
            case "DATEDIF": {
                BigDecimal d1 = parseDateArg();
                match(',');
                BigDecimal d2 = parseDateArg();
                match(',');
                String unit = parseStringArg().toLowerCase(Locale.ROOT);
                match(')');
                if (d1 == null || d2 == null) return null;
                long diffMs = d1.longValue() - d2.longValue();
                long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);
                switch (unit) {
                    case "days":   return BigDecimal.valueOf(diffDays);
                    case "months": return BigDecimal.valueOf(diffDays / 30L);
                    case "years":  return BigDecimal.valueOf(diffDays / 365L);
                    default:       return BigDecimal.valueOf(diffDays);
                }
            }
            case "XDATE.YEAR":
            case "XDATE_YEAR": {
                BigDecimal d = parseDateArg(); match(')');
                if (d == null) return null;
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(new Date(d.longValue()));
                return BigDecimal.valueOf(cal.get(java.util.Calendar.YEAR));
            }
            case "XDATE.MONTH":
            case "XDATE_MONTH": {
                BigDecimal d = parseDateArg(); match(')');
                if (d == null) return null;
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(new Date(d.longValue()));
                return BigDecimal.valueOf(cal.get(java.util.Calendar.MONTH) + 1);
            }
            case "XDATE.MDAY":
            case "XDATE_MDAY": {
                BigDecimal d = parseDateArg(); match(')');
                if (d == null) return null;
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(new Date(d.longValue()));
                return BigDecimal.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH));
            }
            case "NUMBER": {
                String str = parseStringArg();
                match(',');
                parseStringArg(); // format, unused in prototype
                match(')');
                if (str == null || str.trim().isEmpty()) return null;
                try { return new BigDecimal(str.trim()); }
                catch (NumberFormatException e) { return null; }
            }
            case "CHAR.LENGTH":
            case "LENGTH": {
                String str = parseStringArg(); match(')');
                return str == null ? BigDecimal.ZERO : BigDecimal.valueOf(str.length());
            }
            case "CHAR.SUBSTR": {
                String str = parseStringArg(); match(',');
                BigDecimal start = parseAddSubtract(); match(',');
                BigDecimal len = parseAddSubtract(); match(')');
                if (str == null || start == null || len == null) return null;
                int s = start.intValue() - 1; // SPSS is 1-based
                int l = len.intValue();
                if (s < 0 || s >= str.length()) return null;
                int end = Math.min(s + l, str.length());
                // Stored as a pseudo-number (hashCode for later comparison) — this is limited
                // CHAR.SUBSTR is usually used with = "" comparison, handled in ConditionExpression
                return BigDecimal.valueOf(str.substring(s, end).hashCode());
            }
            case "MISSING": {
                // MISSING(var) → 1 if null/empty, 0 otherwise
                String var = parseIdentifierRaw();
                match(')');
                Object val = row.get(var);
                boolean missing = val == null || String.valueOf(val).trim().isEmpty();
                return missing ? BigDecimal.ONE : BigDecimal.ZERO;
            }
            default:
                // Unknown function — skip args and return null
                skipFunctionArgs();
                match(')');
                return null;
        }
    }

    // ── Argument parsing helpers ──────────────────────────────────

    private BigDecimal parseDateArg() {
        skipWhitespace();
        if (peek("$")) { pos++; parseIdentifier(); return null; } // $SYSMIS
        // Try date literal: "dd/mm/yyyy" or variable reference
        if (match('\'') || match('"')) {
            char quote = expression.charAt(pos - 1);
            int start = pos;
            while (pos < expression.length() && expression.charAt(pos) != quote) pos++;
            String dateStr = expression.substring(start, pos);
            pos++; // closing quote
            try {
                Date date = SPSS_DATE.parse(dateStr);
                return BigDecimal.valueOf(date.getTime());
            } catch (ParseException e) {
                return null;
            }
        }
        if (Character.isLetter(expression.charAt(pos)) || expression.charAt(pos) == '_') {
            String var = parseIdentifierRaw();
            Object val = row.get(var);
            if (val == null) return null;
            try {
                Date date = SPSS_DATE.parse(String.valueOf(val).trim());
                return BigDecimal.valueOf(date.getTime());
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

    private String parseStringArg() {
        skipWhitespace();
        if (peek("$")) { pos++; parseIdentifier(); return null; }
        if (match('\'') || match('"')) {
            char quote = expression.charAt(pos - 1);
            int start = pos;
            while (pos < expression.length() && expression.charAt(pos) != quote) pos++;
            String val = expression.substring(start, pos);
            pos++;
            return val;
        }
        if (pos < expression.length() && (Character.isLetter(expression.charAt(pos)) || expression.charAt(pos) == '_')) {
            String name = parseIdentifier();
            skipWhitespace();
            if (match('(')) {
                return evalStringFunction(name, this);
            }
            Object val = row.get(name);
            return val == null ? null : String.valueOf(val).trim();
        }
        return null;
    }

    private String parseIdentifierRaw() {
        // Parse identifier without checking for function call
        int start = pos;
        while (pos < expression.length()) {
            char c = expression.charAt(pos);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.' && Character.UnicodeScript.of(c) != Character.UnicodeScript.HAN) {
                break;
            }
            pos++;
        }
        return expression.substring(start, pos);
    }

    private void skipFunctionArgs() {
        int depth = 1;
        while (pos < expression.length() && depth > 0) {
            char c = expression.charAt(pos);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth > 0) pos++;
        }
    }

    // ── Core parsing ──────────────────────────────────────────────

    private BigDecimal parseNumber() {
        int start = pos;
        while (pos < expression.length()) {
            char c = expression.charAt(pos);
            if (!Character.isDigit(c) && c != '.') {
                break;
            }
            pos++;
        }
        try {
            return new BigDecimal(expression.substring(start, pos));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String parseIdentifier() {
        int start = pos;
        while (pos < expression.length()) {
            char c = expression.charAt(pos);
            if (!Character.isLetterOrDigit(c) && c != '_' && Character.UnicodeScript.of(c) != Character.UnicodeScript.HAN) {
                break;
            }
            pos++;
        }
        return expression.substring(start, pos);
    }

    private boolean match(char expected) {
        skipWhitespace();
        if (pos < expression.length() && expression.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean peek(String expected) {
        skipWhitespace();
        return expression.startsWith(expected, pos);
    }

    private void skipWhitespace() {
        while (pos < expression.length() && Character.isWhitespace(expression.charAt(pos))) {
            pos++;
        }
    }

    // ── Public static helpers used by ConditionExpression ─────────

    /** Evaluate a string-yielding expression (for = "" comparisons) */
    static String evalString(String expression, RowContext row) {
        ArithmeticExpression ae = new ArithmeticExpression(expression, row);
        ae.pos = 0; ae.skipWhitespace();
        if (ae.peek("\"") || ae.peek("'")) {
            // String literal
            char q = ae.expression.charAt(ae.pos);
            ae.pos++;
            int start = ae.pos;
            while (ae.pos < ae.expression.length() && ae.expression.charAt(ae.pos) != q) ae.pos++;
            String val = ae.expression.substring(start, ae.pos);
            ae.pos++;
            return val;
        }
        if (ae.pos < ae.expression.length() && (Character.isLetter(ae.expression.charAt(ae.pos)) || ae.expression.charAt(ae.pos) == '_')) {
            String name = ae.parseIdentifier();
            ae.skipWhitespace();
            if (ae.match('(')) {
                return evalStringFunction(name, ae);
            }
            Object val = row.get(name);
            return val == null ? null : String.valueOf(val).trim();
        }
        return null;
    }

    private static String evalStringFunction(String name, ArithmeticExpression ae) {
        String upper = name.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "LTRIM": {
                String s = ae.parseStringArg(); ae.match(')');
                return s == null ? null : s.replaceAll("^\\s+", "");
            }
            case "RTRIM": {
                String s = ae.parseStringArg(); ae.match(')');
                return s == null ? null : s.replaceAll("\\s+$", "");
            }
            case "LTRIM(RTRIM":
            case "RTRIM(LTRIM":
                return null;
            case "CHAR.SUBSTR": {
                String str = ae.parseStringArg(); ae.match(',');
                BigDecimal start = ae.parseAddSubtract(); ae.match(',');
                BigDecimal len = ae.parseAddSubtract(); ae.match(')');
                if (str == null || start == null || len == null) return null;
                int s = start.intValue() - 1;
                int l = len.intValue();
                if (s < 0 || s >= str.length()) return "";
                int end = Math.min(s + l, str.length());
                return str.substring(s, end);
            }
            case "STRING": {
                BigDecimal num = ae.parseAddSubtract(); ae.match(',');
                ae.parseStringArg(); // format
                ae.match(')');
                return num == null ? null : num.toPlainString();
            }
            case "MISSING": {
                String var = ae.parseIdentifierRaw(); ae.match(')');
                Object val = ae.row.get(var);
                boolean missing = val == null || String.valueOf(val).trim().isEmpty();
                return missing ? "1" : "0";
            }
            default:
                ae.skipFunctionArgs(); ae.match(')');
                return null;
        }
    }

    public static CompiledExpression compile(String expression) {
        return new CompiledExpression(expression);
    }

    /** Check if an expression looks like MISSING(var) — used by ConditionExpression */
    static boolean isMissingCall(String expr) {
        return expr.trim().toUpperCase(Locale.ROOT).matches("MISSING\\s*\\(.*\\)");
    }

    /** Evaluate a MISSING(var) call — returns true if the variable is null/empty */
    static boolean evalMissing(String expr, RowContext row) {
        String trimmed = expr.trim();
        int parenStart = trimmed.indexOf('(');
        int parenEnd = trimmed.lastIndexOf(')');
        if (parenStart < 0 || parenEnd < 0) return false;
        String varName = trimmed.substring(parenStart + 1, parenEnd).trim();
        Object val = row.get(varName);
        return val == null || String.valueOf(val).trim().isEmpty();
    }
}
