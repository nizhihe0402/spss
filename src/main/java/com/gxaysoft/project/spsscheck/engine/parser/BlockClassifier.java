package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import java.util.*;
import java.util.regex.*;

/**
 * SPSS 语法模式 → RuleType 分类器。
 * 分类依据为 SPSS 语法模式，非中文目标名称。
 */
public final class BlockClassifier {
    private static final Set<String> KNOWN_INTERMEDIATE = new HashSet<>(Arrays.asList(
            "ID3", "AGE2", "AGE", "BMI", "BPC", "MATCHSEQUENCE", "INDUPGRP", "SFZ_DATE"
    ));
    private static final Pattern DOC_SOURCE_PAT = Pattern.compile(
            "(?i)\\b(zjtype|zjcard|sfz|card|zjcode|idcard|id_card)\\b");

    private BlockClassifier() {}

    public static RuleType classifyCompute(String target, String expression, List<String> sourceVars) {
        String exprUpper = expression != null ? expression.toUpperCase(Locale.ROOT) : "";
        String targetUpper = target != null ? target.toUpperCase(Locale.ROOT) : "";
        if (KNOWN_INTERMEDIATE.contains(targetUpper)) return RuleType.COMPUTE_INTERMEDIATE;
        if (isIdCheckExpression(exprUpper)) return RuleType.IDENTITY_CHECK;
        if (hasDocSources(sourceVars)) return RuleType.DOCUMENT_CHECK;
        if (exprUpper.matches("^\\s*0\\s*$") || exprUpper.contains("$SYSMIS")) return RuleType.COMPUTE_INTERMEDIATE;
        return RuleType.COMPUTE_INTERMEDIATE;
    }

    public static RuleType classifyRecode(String source, String target, String block) {
        String blockUpper = block != null ? block.toUpperCase(Locale.ROOT) : "";
        if (blockUpper.contains(" THRU ") && (blockUpper.contains("HIGHEST") || blockUpper.contains("LOWEST")))
            return RuleType.OUTCOME_DETERMINATION;
        if (blockUpper.contains("SYSMIS") || blockUpper.contains("MISSING")) return RuleType.MISSING_CHECK;
        if (blockUpper.contains(" THRU ") && !blockUpper.contains("HIGHEST") && !blockUpper.contains("LOWEST"))
            return RuleType.RANGE_CHECK;
        if (DOC_SOURCE_PAT.matcher(source).find() || DOC_SOURCE_PAT.matcher(block).find())
            return RuleType.DOCUMENT_CHECK;
        if (blockUpper.contains(" INTO ")) return RuleType.MISSING_CHECK;
        return RuleType.MISSING_CHECK;
    }

    public static RuleType classifyConditional(List<String> sourceVars, String block) {
        String blockUpper = block != null ? block.toUpperCase(Locale.ROOT) : "";
        if (hasDocSources(sourceVars) || DOC_SOURCE_PAT.matcher(block).find()) return RuleType.DOCUMENT_CHECK;
        if (blockUpper.contains("SYSMIS") || blockUpper.contains("MISSING")) return RuleType.MISSING_CHECK;
        if (blockUpper.contains(" THRU ") && (blockUpper.contains("HIGHEST") || blockUpper.contains("LOWEST")))
            return RuleType.OUTCOME_DETERMINATION;
        return RuleType.CONDITIONAL_BLOCK;
    }

    private static boolean isIdCheckExpression(String expr) {
        boolean hasPowers = expr.contains("**") && (expr.contains("10") || expr.contains("PROVINCE") || expr.contains("CITY"));
        boolean hasIdVars = expr.contains("PROVINCE") || expr.contains("CITY") || expr.contains("COUNTY")
                || expr.contains("POINT") || expr.contains("SCHOOL");
        return hasPowers && hasIdVars;
    }

    private static boolean hasDocSources(Collection<String> vars) {
        if (vars == null) return false;
        for (String v : vars) if (DOC_SOURCE_PAT.matcher(v).find()) return true;
        return false;
    }
}
