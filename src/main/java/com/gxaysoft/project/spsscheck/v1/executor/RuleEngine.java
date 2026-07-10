package com.gxaysoft.project.spsscheck.v1.executor;

import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.v1.model.*;
import com.gxaysoft.project.spsscheck.model.RowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/** V1 legacy Step-based execution engine. */
public final class RuleEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private RuleEngine() {
    }

    public static void execute(List<RowContext> rows, List<SpssCheckRule> rules) {
        log.info("开始执行规则: rows={}, rules={}", rows.size(), rules.size());
        for (RowContext row : rows) {
            for (SpssCheckRule rule : rules) {
                if (rule.getSteps().isEmpty()) {
                    BigDecimal computed = new ArithmeticExpression(rule.getExpression(), row).parse();
                    if (rule.isCheckRule()) {
                        int flag = computed == null || computed.compareTo(BigDecimal.ZERO) != 0 ? 1 : 0;
                        row.put(rule.getTarget(), flag);
                        row.putFlag(rule.getTarget(), flag);
                    } else {
                        row.put(rule.getTarget(), computed);
                    }
                } else {
                    for (RuleStep step : rule.getSteps()) {
                        step.execute(row);
                    }
                    if (rule.isCheckRule()) {
                        BigDecimal value = row.getDecimal(rule.getTarget());
                        int flag = value != null && value.compareTo(BigDecimal.ZERO) != 0 ? 1 : 0;
                        row.put(rule.getTarget(), flag);
                        row.putFlag(rule.getTarget(), flag);
                    }
                }
            }
        }
    }
}
