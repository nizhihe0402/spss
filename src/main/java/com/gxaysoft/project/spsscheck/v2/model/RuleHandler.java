package com.gxaysoft.project.spsscheck.v2.model;

import com.gxaysoft.project.spsscheck.model.RowContext;

/** 每种 RuleType 对应一个 Handler，负责该类型规则的执行逻辑 */
public interface RuleHandler {
    /** 此 Handler 处理的规则类型 */
    RuleType handles();

    /** 对一行数据执行规则，返回 0/1 标识（1=异常）或计算结果 */
    void execute(RuleDefinition rule, RowContext row);

    /** 从 RuleDefinition 提取人类可读的说明 */
    String describe(RuleDefinition rule);
}
