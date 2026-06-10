package com.gxaysoft.project.spsscheck.v1.model;
import com.gxaysoft.project.spsscheck.model.RowContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpssDatasetRule {
    private final String sortVariable;
    private final String byVariable;
    private final String firstVariable;
    private final String lastVariable;
    private final String spssSource;
    private final String javaRule;

    public SpssDatasetRule(String sortVariable, String byVariable, String firstVariable, String lastVariable, String spssSource) {
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

    public void execute(List<RowContext> rows) {
        rows.sort(new Comparator<RowContext>() {
            @Override
            public int compare(RowContext left, RowContext right) {
                BigDecimal l = left.getDecimal(sortVariable);
                BigDecimal r = right.getDecimal(sortVariable);
                if (l == null && r == null) {
                    return 0;
                }
                if (l == null) {
                    return -1;
                }
                if (r == null) {
                    return 1;
                }
                return l.compareTo(r);
            }
        });

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
