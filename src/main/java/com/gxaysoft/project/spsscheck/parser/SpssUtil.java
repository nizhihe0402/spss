package com.gxaysoft.project.spsscheck.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpssUtil {
    private SpssUtil() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final Set<String> KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "COMPUTE", "EXECUTE", "RECODE", "SYSMIS", "ELSE", "THRU", "INTO",
            "VARIABLE", "VALUE", "LABELS", "IF", "DO", "END", "AND", "OR",
            "DATEDIFF", "DATEDIF", "CHAR", "MAX", "MIN", "SUM", "MEAN",
            "DAYS", "MONTHS", "YEARS", "HOURS", "MINUTES", "SECONDS",
            "NUMBER", "STRING", "MISSING", "RND", "MOD", "NOT",
            "XDATE", "LTRIM", "RTRIM", "LENGTH", "SUBSTR",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10",
            "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10",
            "LOWEST", "HIGHEST", "THRU", "COPY",
            "YEAR", "MONTH", "MDAY", "SUBSTR", "ORDINAL", "SCALE", "LAYERED",
            "ANALYSIS", "FORMAT", "LEAVE", "SPLIT", "FILE", "OFF",
            "NOTABLE", "STATISTICS", "STDDEV", "MINIMUM", "MAXIMUM", "MEDIAN",
            "COMPRESSED", "OUTFILE", "DROP", "FIRST", "LAST", "PRIMARY", "DUPLICATE",
            "DISPLAY", "LABEL", "CATEGORIES", "KEY", "VALUE", "EMPTY", "EXCLUDE", "INCLUDE",
            "SELECT", "SAVE", "CASES", "MATCH", "ALL", "KEEP", "MAP", "RENAME", "MAKE",
            "TABLE", "VLABELS", "VARIABLES", "COUNT", "COLUMN", "ROW", "TOTAL", "POSITION",
            "TITLE", "SUBTITLE", "FOOTNOTE", "TEMPORARY", "FILTER", "DATASET", "NAME", "WINDOW",
            "ACTIVATE", "CLOSE", "SORT", "ASCENDING", "DESCENDING", "TO", "WITH"
    ));

    public static List<String> extractVariables(String expression) {
        String cleaned = stripCommentsAndStringLiterals(expression);
        // Match SPSS identifiers: optional # prefix for scratch variables, then letters/digits/underscores/Chinese
        Matcher matcher = Pattern.compile("#?[\\p{L}_][\\p{L}\\p{N}_]*").matcher(cleaned);
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        while (matcher.find()) {
            String variable = matcher.group();
            String normalized = normalize(variable);
            if (isLikelyVariable(variable, normalized)) {
                variables.put(normalized, variable.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(variables.values());
    }

    /**
     * Remove SPSS human comments and string literals before variable extraction.
     * Otherwise Chinese descriptions, value labels and file names are easily misread as source variables.
     */
    private static String stripCommentsAndStringLiterals(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder noComments = new StringBuilder();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String line : lines) {
            String trimmed = line.replace("﻿", "").trim();
            if (trimmed.startsWith("*")) continue;
            if (noComments.length() > 0) noComments.append('\n');
            noComments.append(line);
        }

        StringBuilder out = new StringBuilder(noComments.length());
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < noComments.length(); i++) {
            char c = noComments.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                out.append(' ');
            } else if (c == '\"' && !inSingle) {
                inDouble = !inDouble;
                out.append(' ');
            } else if (inSingle || inDouble) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isLikelyVariable(String variable, String normalized) {
        if (variable == null || variable.trim().isEmpty()) return false;
        if (normalized == null || normalized.isEmpty()) return false;
        if (KEYWORDS.contains(normalized)) return false;
        if (variable.startsWith("$")) return false;
        if (variable.length() > 64) return false;
        if (variable.indexOf('\\') >= 0 || variable.indexOf('/') >= 0 || variable.indexOf(':') >= 0) return false;
        // Function names frequently appear before '(' and should not become variables.
        if (Arrays.asList("TRUNC", "ABS", "SQRT", "EXP", "LN", "LG10", "ANY", "RANGE", "CHAR",
                "CONCAT", "INDEX", "DATE", "TIME", "VALUE", "LAG", "VALUELABEL").contains(normalized)) {
            return false;
        }
        return true;
    }
}
