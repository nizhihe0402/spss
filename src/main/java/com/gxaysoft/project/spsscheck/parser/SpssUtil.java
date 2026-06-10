package com.gxaysoft.project.spsscheck.parser;

import java.util.Locale;

public final class SpssUtil {
    private SpssUtil() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
