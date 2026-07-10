package com.gxaysoft.project.spsscheck.engine.executor;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 行级规则执行器。按顺序对每行执行规则列表。
 * 从 v1 RuleEngine 迁移，但使用统一的 engine.model.Rule。
 */
public final class RuleExecutor {
    private static final Logger log = LoggerFactory.getLogger(RuleExecutor.class);

    private RuleExecutor() {}

    public static void execute(List<RowContext> rows, List<Rule> rules) {
        log.info("开始执行规则: rows={}, rules={}", rows.size(), rules.size());

        for (RowContext row : rows) {
            for (Rule rule : rules) {
                if (rule.getSteps().isEmpty()) {
                    // 无 Step — 使用旧的简化模式
                    BigDecimal computed = new ArithmeticExpression(rule.getExpression(), row).parse();
                    if (rule.isCheckRule()) {
                        int flag = (computed == null || computed.compareTo(BigDecimal.ZERO) != 0) ? 1 : 0;
                        row.put(rule.getTarget(), flag);
                        row.putFlag(rule.getTarget(), flag);
                    } else {
                        row.put(rule.getTarget(), computed);
                    }
                } else {
                    // 有 Step — 扁平化执行（条件在解析时已解析，无需运行时包装！）
                    for (Step step : rule.getSteps()) {
                        step.execute(row);
                    }
                    if (rule.isCheckRule()) {
                        BigDecimal value = row.getDecimal(rule.getTarget());
                        int flag = (value != null && value.compareTo(BigDecimal.ZERO) != 0) ? 1 : 0;
                        row.put(rule.getTarget(), flag);
                        row.putFlag(rule.getTarget(), flag);
                    }
                }
            }
        }

        log.info("规则执行完毕: rows={}", rows.size());
    }
}
