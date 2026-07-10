package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.parser.SpssUtil;
import java.util.*;

public class Step {
    private final String condition;  // null = unconditional
    private final StepAction action;

    public Step(String condition, StepAction action) {
        this.condition = condition; this.action = action;
    }

    public void execute(RowContext row) {
        if (condition == null || new ConditionExpression(condition, row).eval()) {
            action.execute(row);
        }
    }

    public String getCondition() { return condition; }
    public StepAction getAction() { return action; }
    public String getTarget() { return action.target(); }

    public List<String> sourceVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        if (condition != null) {
            for (String v : SpssUtil.extractVariables(condition)) {
                vars.put(SpssUtil.normalize(v), v.toUpperCase(Locale.ROOT));
            }
        }
        for (String v : action.sourceVariables()) {
            vars.put(SpssUtil.normalize(v), v.toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(vars.values());
    }
}
