package com.gxaysoft.project.spsscheck.web;

import java.util.Collections;
import java.util.Map;

/** DTO for correction update request body parsing (supports both camelCase and snake_case keys). */
public class CorrectionUpdate {
    public final int enabled;
    public final String type;
    public final String variables;
    public final String source;
    public final String strategy;
    public final String applyStage;
    public final int writeClean;
    public final int writeSource;
    public final String description;

    private CorrectionUpdate(int enabled,
                             String type,
                             String variables,
                             String source,
                             String strategy,
                             String applyStage,
                             int writeClean,
                             int writeSource,
                             String description) {
        this.enabled = enabled;
        this.type = type;
        this.variables = variables;
        this.source = source;
        this.strategy = strategy;
        this.applyStage = applyStage;
        this.writeClean = writeClean;
        this.writeSource = writeSource;
        this.description = description;
    }

    public static CorrectionUpdate from(Map<String, Object> body) {
        Map<String, Object> values = body == null ? Collections.<String, Object>emptyMap() : body;
        return new CorrectionUpdate(
                boolInt(value(values, "correctionEnabled", "correction_enabled")),
                text(value(values, "correctionType", "correction_type")),
                text(value(values, "correctionVariables", "correction_variables")),
                text(value(values, "correctionSource", "correction_source")),
                text(value(values, "correctionStrategy", "correction_strategy")),
                text(value(values, "correctionApplyStage", "correction_apply_stage")),
                boolInt(value(values, "correctionWriteClean", "correction_write_clean")),
                boolInt(value(values, "correctionWriteSource", "correction_write_source")),
                text(value(values, "correctionDescription", "correction_description")));
    }

    private static Object value(Map<String, Object> body, String camel, String snake) {
        return body.containsKey(camel) ? body.get(camel) : body.get(snake);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int boolInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue() == 0 ? 0 : 1;
        if (value instanceof Boolean) return ((Boolean) value).booleanValue() ? 1 : 0;
        String text = String.valueOf(value).trim();
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)
                || "on".equalsIgnoreCase(text) ? 1 : 0;
    }
}
