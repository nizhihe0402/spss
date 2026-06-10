package com.gxaysoft.project.spsscheck.model;

import java.math.BigDecimal;

public class RecodeCase {
    private final String type;
    private final String from;
    private final String to;
    private final String result;

    private RecodeCase(String type, String from, String to, String result) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.result = result;
    }

    public static RecodeCase missing(String result) {
        return new RecodeCase("missing", null, null, result);
    }

    public static RecodeCase elseCase(String result) {
        return new RecodeCase("else", null, null, result);
    }

    public static RecodeCase range(String from, String to, String result) {
        return new RecodeCase("range", from, to, result);
    }

    public static RecodeCase equalsCase(String from, String result) {
        return new RecodeCase("equals", from, null, result);
    }

    public boolean matches(Object value) {
        if ("else".equals(type)) {
            return true;
        }
        if ("missing".equals(type)) {
            return value == null || String.valueOf(value).trim().isEmpty();
        }
        BigDecimal actual = toDecimal(value);
        if (actual == null) {
            return false;
        }
        if ("equals".equals(type)) {
            BigDecimal expected = toDecimal(from);
            return expected != null && actual.compareTo(expected) == 0;
        }
        if ("range".equals(type)) {
            BigDecimal min = toDecimal(from);
            BigDecimal max = toDecimal(to);
            return min != null && max != null && actual.compareTo(min) >= 0 && actual.compareTo(max) <= 0;
        }
        return false;
    }

    public String toValue() {
        return result;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
