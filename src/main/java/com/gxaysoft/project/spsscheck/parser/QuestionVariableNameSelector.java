package com.gxaysoft.project.spsscheck.parser;

public final class QuestionVariableNameSelector {
    private QuestionVariableNameSelector() {
    }

    public static String variableNameFromExportContent(String exportContent) {
        if (looksLikeSpssVariable(exportContent)) {
            return exportContent.trim();
        }
        return null;
    }

    public static boolean looksLikeSpssVariable(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.matches("[A-Za-z][A-Za-z0-9_]*");
    }
}
