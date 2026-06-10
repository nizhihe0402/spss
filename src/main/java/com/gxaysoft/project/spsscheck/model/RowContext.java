package com.gxaysoft.project.spsscheck.model;

import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class RowContext {
    private final String sampleKey;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, Integer> flags = new LinkedHashMap<>();

    public RowContext(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public void put(String variable, Object value) {
        values.put(SpssUtil.normalize(variable), value);
    }

    public Object get(String variable) {
        return values.get(SpssUtil.normalize(variable));
    }

    public BigDecimal getDecimal(String variable) {
        Object value = get(variable);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public void putFlag(String variable, int flag) {
        flags.put(SpssUtil.normalize(variable), flag);
    }

    public int getFlag(String variable) {
        Integer flag = flags.get(SpssUtil.normalize(variable));
        return flag == null ? 0 : flag;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public Map<String, Integer> getFlags() {
        return flags;
    }
}
