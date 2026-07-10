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

    public boolean isAvailable(Rule rule) {
        if (rule.getSourceVariables() == null) return true;
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
