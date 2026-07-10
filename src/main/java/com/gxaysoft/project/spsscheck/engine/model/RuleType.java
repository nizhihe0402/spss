package com.gxaysoft.project.spsscheck.engine.model;

public enum RuleType {
    COMPUTE_INTERMEDIATE("中间计算"),
    IDENTITY_CHECK("ID校验"),
    DUPLICATE_MARK("去重标记"),
    MISSING_CHECK("缺失检查"),
    CONSISTENCY_CHECK("一致性校验"),
    RANGE_CHECK("范围校验"),
    DOCUMENT_CHECK("证件校验"),
    CONDITIONAL_BLOCK("条件块"),
    OUTPUT_GROUP("输出分组"),
    OUTCOME_DETERMINATION("结局判定");

    public final String label;
    RuleType(String label) { this.label = label; }
}
