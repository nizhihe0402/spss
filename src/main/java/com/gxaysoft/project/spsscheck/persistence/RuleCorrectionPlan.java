package com.gxaysoft.project.spsscheck.persistence;

public final class RuleCorrectionPlan {
    public final boolean enabled;
    public final String type;
    public final String variables;
    public final String source;
    public final String strategy;
    public final String applyStage;
    public final boolean writeClean;
    public final boolean writeSource;
    public final String description;

    private RuleCorrectionPlan(boolean enabled,
                               String type,
                               String variables,
                               String source,
                               String strategy,
                               String applyStage,
                               boolean writeClean,
                               boolean writeSource,
                               String description) {
        this.enabled = enabled;
        this.type = empty(type);
        this.variables = empty(variables);
        this.source = empty(source);
        this.strategy = empty(strategy);
        this.applyStage = empty(applyStage);
        this.writeClean = writeClean;
        this.writeSource = writeSource;
        this.description = empty(description);
    }

    public static RuleCorrectionPlan none() {
        return new RuleCorrectionPlan(false, "", "", "", "", "", false, false, "");
    }

    public static RuleCorrectionPlan detect(String ruleType,
                                            String target,
                                            String sources,
                                            String description) {
        String normalizedType = upper(ruleType);
        String normalizedTarget = upper(target);
        String normalizedSources = upper(sources);
        String text = empty(target) + " " + empty(description);

        if (isSchoolMissingRule(normalizedTarget, normalizedSources, text)) {
            return new RuleCorrectionPlan(
                    true,
                    "FILL_SCHOOL_CODE",
                    "SCHOOL",
                    "bus_student.school_id -> bus_school.school_code",
                    "SCHOOL缺失时，从学生学校信息补学校编码",
                    "BEFORE_RULE_EXECUTION",
                    true,
                    false,
                    "SCHOOL缺失：从学生学校信息补学校编码，clean保存纠偏值");
        }

        if (isIdentityRule(normalizedType, normalizedTarget, text)) {
            return new RuleCorrectionPlan(
                    true,
                    "NORMALIZE_REGION_CODE",
                    "PROVINCE,CITY,COUNTY",
                    "规则源变量",
                    "省市区编码非2位时取右2位参与ID3计算",
                    "BEFORE_RULE_EXECUTION",
                    true,
                    false,
                    "ID一致性：省市区非2位时取右2位参与校验，clean保存2位编码");
        }

        boolean documentRule = isDocumentRule(normalizedType, normalizedTarget, text);
        String documentVariables = documentVariables(normalizedTarget, normalizedSources, text, documentRule);
        if (!documentVariables.isEmpty() && (documentRule || isDocumentVariableTarget(normalizedTarget))) {
            return new RuleCorrectionPlan(
                    true,
                    "FILL_DOCUMENT_INFO",
                    documentVariables,
                    "bus_student.id_type/card 或学生证件JSON",
                    "证件信息缺失时，从学生信息补齐",
                    "BEFORE_RULE_EXECUTION",
                    true,
                    false,
                    "证件信息缺失：从学生信息补齐证件类型/证件号码，clean保存纠偏值");
        }

        return none();
    }

    private static boolean isSchoolMissingRule(String target, String sources, String text) {
        return (target.contains("SCHOOL") || text.contains("学校编码缺失") || text.contains("瀛︽牎缂栫爜缂哄け"))
                && containsVariable(sources, "SCHOOL");
    }

    private static boolean isIdentityRule(String type, String target, String text) {
        return type.contains("IDENTITY_CHECK")
                || text.contains("ID是否一致")
                || text.contains("ID鏄惁涓€鑷")
                || target.contains("ID") && (target.contains("一致") || target.contains("涓€鑷"));
    }

    private static boolean isDocumentRule(String type, String target, String text) {
        return type.contains("DOCUMENT_CHECK")
                || target.contains("ZJTYPE")
                || isDocumentVariableTarget(target)
                || text.contains("证件")
                || text.contains("身份证")
                || text.contains("璇佷欢")
                || text.contains("韬唤璇");
    }

    private static String documentVariables(String target, String sources, String text, boolean documentRule) {
        if (isDocumentNumberMissingText(text)) {
            return "ZJTYPE,SFZ,MTP,TRPMT,HZ";
        }
        StringBuilder result = new StringBuilder();
        appendVariable(result, target.contains("ZJTYPE") || documentRule && sources.contains("ZJTYPE"), "ZJTYPE");
        appendVariable(result, target.contains("SFZ") || documentRule && sources.contains("SFZ"), "SFZ");
        appendVariable(result, target.contains("MTP") || documentRule && sources.contains("MTP"), "MTP");
        appendVariable(result, target.contains("TRPMT") || documentRule && sources.contains("TRPMT"), "TRPMT");
        appendVariable(result, target.contains("HZ") || documentRule && sources.contains("HZ"), "HZ");
        return result.toString();
    }

    private static boolean isDocumentNumberMissingText(String text) {
        return text.contains("证件号码缺失")
                || text.contains("证件号缺失")
                || text.contains("璇佷欢鍙风爜缂哄け")
                || text.contains("璇佷欢鍙风己澶");
    }

    private static boolean isDocumentVariableTarget(String target) {
        return target.contains("SFZ")
                || target.contains("MTP")
                || target.contains("TRPMT")
                || target.contains("HZ");
    }

    private static void appendVariable(StringBuilder result, boolean enabled, String variable) {
        if (!enabled) return;
        if (result.length() > 0) result.append(",");
        result.append(variable);
    }

    private static boolean containsVariable(String sources, String variable) {
        if (sources == null || sources.trim().isEmpty()) return false;
        String[] parts = sources.split("[,，\\s]+");
        for (String part : parts) {
            if (variable.equals(upper(part))) return true;
        }
        return false;
    }

    private static String upper(String value) {
        return empty(value).trim().toUpperCase();
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }
}
