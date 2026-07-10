package com.gxaysoft.project.spsscheck.engine.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * DO IF 条件栈 — 将嵌套的 DO IF ... ELSE ... END IF 扁平化。
 *
 * 算法核心：
 * 1. 遇到 DO IF(cond) → push cond 到栈顶
 * 2. 遇到 COMPUTE/RECODE/IF → 将栈中所有条件以 AND 串联，生成一个 Step
 * 3. 遇到 ELSE → pop 栈顶条件 C，push NOT(C)
 * 4. 遇到 END IF → pop 栈顶
 *
 * 最大实测深度：3（仅出现在身份证号校验逻辑中）。
 */
public class ConditionStack {
    private final List<String> stack;

    public ConditionStack() {
        this.stack = new ArrayList<>();
    }

    public ConditionStack(ConditionStack other) {
        this.stack = new ArrayList<>(other.stack);
    }

    public void pushDoIf(String condition) {
        if (condition == null || condition.trim().isEmpty()) return;
        stack.add(condition.trim());
    }

    public void applyElse() {
        if (stack.isEmpty()) return;
        String last = stack.remove(stack.size() - 1);
        String negated = "NOT(" + last + ")";
        stack.add(negated);
    }

    public void popEndIf() {
        if (!stack.isEmpty()) stack.remove(stack.size() - 1);
    }

    public String buildCondition() {
        if (stack.isEmpty()) return null;
        if (stack.size() == 1) return stack.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(stack.get(i));
        }
        return sb.toString();
    }

    public int depth() { return stack.size(); }
    public boolean isEmpty() { return stack.isEmpty(); }
}
