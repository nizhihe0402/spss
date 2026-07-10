package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
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
            row.put(target, value);
        }
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
