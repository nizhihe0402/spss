package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SourceQuestionMappingFormatter {
    private SourceQuestionMappingFormatter() {
    }

    public static String format(String sources, Map<String, QuestionMapping> mappings) {
        List<String> lines = new ArrayList<String>();
        for (String source : splitSources(sources)) {
            QuestionMapping mapping = mappings == null ? null : mappings.get(SpssUtil.normalize(source));
            lines.add(source + " -> " + (mapping == null || mapping.getQuestionId() <= 0 ? "-" : String.valueOf(mapping.getQuestionId())));
        }
        return joinLines(lines);
    }

    public static String formatLookup(String sources, Map<String, List<QuestionMapping>> mappings) {
        List<String> lines = new ArrayList<String>();
        for (String source : splitSources(sources)) {
            List<QuestionMapping> sourceMappings = mappings == null ? null : mappings.get(SpssUtil.normalize(source));
            lines.add(source + " -> " + questionIds(sourceMappings));
        }
        return joinLines(lines);
    }

    private static List<String> splitSources(String sources) {
        List<String> result = new ArrayList<String>();
        if (sources == null || sources.trim().isEmpty()) return result;
        String[] parts = sources.split(",");
        Set<String> seen = new LinkedHashSet<String>();
        for (String part : parts) {
            String source = part == null ? "" : part.trim();
            if (source.isEmpty()) continue;
            String key = SpssUtil.normalize(source);
            if (seen.add(key)) {
                result.add(source);
            }
        }
        return result;
    }

    private static String questionIds(Collection<QuestionMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) return "-";
        Set<String> ids = new LinkedHashSet<String>();
        for (QuestionMapping mapping : mappings) {
            if (mapping != null && mapping.getQuestionId() > 0) {
                ids.add(String.valueOf(mapping.getQuestionId()));
            }
        }
        return ids.isEmpty() ? "-" : join(ids, "/");
    }

    private static String joinLines(List<String> lines) {
        return join(lines, "\n");
    }

    private static String join(Collection<String> values, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String value : values) {
            if (!first) sb.append(delimiter);
            sb.append(value);
            first = false;
        }
        return sb.toString();
    }
}
