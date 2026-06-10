package com.gxaysoft.project.spsscheck.v2.handler;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v2.model.*;

/** SELECT IF + SAVE OUTFILE → output grouping */
public class OutputGroupHandler implements RuleHandler {
    @Override public RuleType handles() { return RuleType.OUTPUT_GROUP; }

    @Override
    public void execute(RuleDefinition rule, RowContext row) {
        // Output rules don't modify rows — they filter rows into groups
    }

    @Override
    public String describe(RuleDefinition rule) {
        String name = rule.getOutputName();
        if (name != null && name.contains("清理后")) return "清洗后数据输出";
        return "异常分组：" + (name != null ? name : rule.getSelectCondition());
    }

    /** Check if a row matches this output group's SELECT IF condition */
    public boolean matches(RuleDefinition rule, RowContext row) {
        if (rule.getSelectCondition() == null) return false;
        return new ConditionExpression(rule.getSelectCondition(), row).eval();
    }
}
