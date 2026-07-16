package com.gxaysoft.project.spsscheck.engine.executor;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;

import java.util.*;

/**
 * 变量可用性链检查器。
 * 规则执行顺序依赖变量可用性：DB 列 → 前序规则的目标 → 数据集规则变量。
 */
public final class AvailabilityChecker {
    private final Set<String> availableVariables;

    public AvailabilityChecker(Set<String> dbColumns) {
        this.availableVariables = new LinkedHashSet<>();
        for (String col : dbColumns) availableVariables.add(SpssUtil.normalize(col));
    }

    public void addRule(Rule rule) {
        if (rule.getTarget() != null) availableVariables.add(SpssUtil.normalize(rule.getTarget()));
    }

    public void addDatasetVariables(String... vars) {
        for (String var : vars) availableVariables.add(SpssUtil.normalize(var));
    }

    /**
     * 宽松可用性：规则只要至少有一个步骤的源变量全部可用即可执行。
     * 适用于归组规则（合并后源变量并集可能超出测试 fixture 列），
     * 不可用步骤在 {@link com.gxaysoft.project.spsscheck.engine.model.Step#execute}
     * 中因缺失变量导致条件求值为 false 而自然变为 no-op。
     * 无步骤的规则（只有 expression）回落到原严格判定。
     */
    public boolean isAvailable(Rule rule) {
        if (rule.getSourceVariables() == null) return true;
        // 有步骤 → 只要至少一个步骤的源变量全部可用即可执行
        if (rule.getSteps() != null && !rule.getSteps().isEmpty()) {
            for (com.gxaysoft.project.spsscheck.engine.model.Step step : rule.getSteps()) {
                boolean stepOk = true;
                for (String var : step.sourceVariables()) {
                    if (!availableVariables.contains(SpssUtil.normalize(var))) {
                        stepOk = false;
                        break;
                    }
                }
                if (stepOk) return true; // 至少一步完全可用
            }
            return false; // 所有步骤都缺变量
        }
        // 无步骤 → 原严格判定
        for (String var : rule.getSourceVariables()) {
            if (!availableVariables.contains(SpssUtil.normalize(var))) return false;
        }
        return true;
    }

    public List<Rule> filterAvailable(List<Rule> rules) {
        List<Rule> available = new ArrayList<>();
        for (Rule rule : rules) {
            if (isAvailable(rule)) { available.add(rule); addRule(rule); }
        }
        return available;
    }

    public Set<String> getAvailableVariables() { return availableVariables; }
}
