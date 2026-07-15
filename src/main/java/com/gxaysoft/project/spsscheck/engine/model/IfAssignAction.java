package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.ArithmeticExpression;
import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import java.math.BigDecimal;
import java.util.*;

public class IfAssignAction implements StepAction {
    private final String condition;
    private final String target;
    private final String value;

    public IfAssignAction(String condition, String target, String value) {
        this.condition = condition; this.target = target; this.value = value;
    }

    @Override
    public void execute(RowContext row) {
        if (new ConditionExpression(condition, row).eval()) {
            row.put(target, evaluateValue(row));
        }
    }

    /**
     * 求值赋值右侧：带引号的字符串字面量按原文（去引号）存；否则按算术表达式求值，
     * 支持 y+1 这类非常量右侧。求值失败（如引用缺失变量）保底存原始字符串，
     * 行为不劣于旧实现。
     */
    private Object evaluateValue(RowContext row) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return value;
        if (v.length() >= 2
                && ((v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"')
                 || (v.charAt(0) == '\'' && v.charAt(v.length() - 1) == '\''))) {
            return v.substring(1, v.length() - 1);
        }
        BigDecimal n = new ArithmeticExpression(v, row).parse();
        return n != null ? n : value;
    }

    @Override public String target() { return target; }
    public String getCondition() { return condition; }
    public String getValue() { return value; }

    @Override
    public List<String> sourceVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        for (String v : SpssUtil.extractVariables(condition)) {
            String norm = SpssUtil.normalize(v);
            if (!norm.equals(SpssUtil.normalize(target))) vars.put(norm, v.toUpperCase(Locale.ROOT));
        }
        if (value != null && !value.matches("[+-]?\\d+(\\.\\d+)?")) {
            for (String v : SpssUtil.extractVariables(value)) {
                String norm = SpssUtil.normalize(v);
                if (!norm.equals(SpssUtil.normalize(target))) vars.put(norm, v.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(vars.values());
    }
}
