package com.gxaysoft.project.spsscheck.v2.model;

public enum RuleType {
    /** 计算中间变量：ID3, age2, BMI, BPC */
    COMPUTE_INTERMEDIATE("中间计算"),
    /** ID一致性校验：ID1 vs 分项编码 */
    IDENTITY_CHECK("ID校验"),
    /** 去重标记：SORT CASES + MATCH FILES */
    DUPLICATE_MARK("去重标记"),
    /** 基本信息缺失/异常检查 */
    MISSING_CHECK("缺失检查"),
    /** 字段逻辑一致性：人员配备、经费、学生情况 */
    CONSISTENCY_CHECK("一致性校验"),
    /** 数值范围校验：身高、体重、血压、年龄 */
    RANGE_CHECK("范围校验"),
    /** 证件校验：号码缺失、位数异常、出生日期不一致 */
    DOCUMENT_CHECK("证件校验"),
    /** 条件计算块：DO IF + COMPUTE + RECODE */
    CONDITIONAL_BLOCK("条件块"),
    /** 输出分组：SELECT IF + SAVE OUTFILE */
    OUTPUT_GROUP("输出分组"),
    /** 结局判定：超重/肥胖/血压偏高 */
    OUTCOME_DETERMINATION("结局判定");

    public final String label;
    RuleType(String label) { this.label = label; }
}
