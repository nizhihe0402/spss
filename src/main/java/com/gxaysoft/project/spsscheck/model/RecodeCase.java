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
            BigDecimal min = isLowest(from) ? null : toDecimal(from);
            BigDecimal max = isHighest(to) ? null : toDecimal(to);
            boolean aboveMin = min == null || actual.compareTo(min) >= 0;
            boolean belowMax = max == null || actual.compareTo(max) <= 0;
            return aboveMin && belowMax;
        }
        return false;
    }

    public Object toValue(Object sourceValue) {
        if (isCopyResult()) {
            return sourceValue;
        }
        return result;
    }

    public String toValue() {
        return result;
    }

    public boolean isCopyResult() {
        return result != null && "COPY".equalsIgnoreCase(result.trim());
    }

    public boolean isZeroOneResult() {
        BigDecimal decimal = toDecimal(result);
        return decimal != null
                && (decimal.compareTo(BigDecimal.ZERO) == 0 || decimal.compareTo(BigDecimal.ONE) == 0);
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

    private boolean isLowest(String value) {
        return value != null && "LOWEST".equalsIgnoreCase(value.trim());
    }

    private boolean isHighest(String value) {
        return value != null && "HIGHEST".equalsIgnoreCase(value.trim());
    }
}
