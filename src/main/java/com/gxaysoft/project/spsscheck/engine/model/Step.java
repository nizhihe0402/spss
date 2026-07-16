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

    /** 人类可读的 Java 伪代码片段 */
    public String javaPreview() {
        StringBuilder sb = new StringBuilder();
        if (condition != null) {
            sb.append("IF(").append(condition).append(") ");
        }
        if (action instanceof ComputeAction) {
            ComputeAction ca = (ComputeAction) action;
            sb.append("COMPUTE ").append(getTarget()).append(" = ").append(ca.getExpression());
        } else if (action instanceof RecodeAction) {
            RecodeAction ra = (RecodeAction) action;
            sb.append("RECODE ").append(ra.getSource());
            if (ra.getCases() != null && !ra.getCases().isEmpty()) {
                sb.append("(");
                for (int i = 0; i < ra.getCases().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(ra.getCases().get(i).toDisplayString());
                }
                sb.append(")");
            }
            sb.append(" → ").append(getTarget());
            if (ra.alwaysWrites()) {
                sb.append(" → 覆盖").append(getTarget());
            }
        } else if (action instanceof IfAssignAction) {
            IfAssignAction ia = (IfAssignAction) action;
            sb.append("IF(").append(ia.getCondition()).append(") ").append(getTarget()).append(" = ").append(ia.getValue());
        }
        return sb.toString();
    }
}
