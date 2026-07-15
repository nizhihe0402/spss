package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;


import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatasetRule {
    private final String sortVariable;
    private final String byVariable;
    private final String firstVariable;
    private final String lastVariable;
    private final String spssSource;
    private final String javaRule;

    public DatasetRule(String sortVariable, String byVariable, String firstVariable, String lastVariable, String spssSource) {
        this.sortVariable = sortVariable;
        this.byVariable = byVariable;
        this.firstVariable = firstVariable;
        this.lastVariable = lastVariable;
        this.spssSource = spssSource;
        this.javaRule = "groupBy(" + byVariable + "); first -> " + firstVariable + "; last -> " + lastVariable;
    }

    public String getByVariable() {
        return byVariable;
    }

    public String getFirstVariable() {
        return firstVariable;
    }

    public String getLastVariable() {
        return lastVariable;
    }

    public String getSpssSource() {
        return spssSource;
    }

    public String getJavaRule() {
        return javaRule;
    }

    /**
     * 重复样本标记：按 BY 变量保序分组，组内首条 FIRST=1、末条 LAST=1，其余 0。
     * 等价于 SPSS 的 SORT CASES + MATCH FILES FIRST/LAST 语义（FIRST/LAST 标记
     * 只依赖分组，不依赖排序），且不改变行的原始顺序——原实现的 rows.sort()
     * 会打乱后续结果的展示顺序。
     */
    public void execute(List<RowContext> rows) {
        Map<String, List<RowContext>> groups = new LinkedHashMap<>();
        for (RowContext row : rows) {
            Object value = row.get(byVariable);
            String key = value == null ? "" : String.valueOf(value);
            List<RowContext> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(key, group);
            }
            group.add(row);
        }
        for (List<RowContext> group : groups.values()) {
            for (int i = 0; i < group.size(); i++) {
                RowContext row = group.get(i);
                row.put(firstVariable, i == 0 ? 1 : 0);
                row.put(lastVariable, i == group.size() - 1 ? 1 : 0);
            }
        }
    }
}
