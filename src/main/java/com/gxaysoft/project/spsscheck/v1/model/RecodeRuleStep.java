package com.gxaysoft.project.spsscheck.v1.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.model.RecodeCase;

import java.util.Collections;
import java.util.List;

public class RecodeRuleStep implements RuleStep {
    private final String source;
    private final String target;
    private final List<RecodeCase> cases;

    public RecodeRuleStep(String source, String target, List<RecodeCase> cases) {
        this.source = source;
        this.target = target;
        this.cases = cases;
    }

    @Override
    public void execute(RowContext row) {
        Object value = row.get(source);
        for (RecodeCase recodeCase : cases) {
            if (recodeCase.matches(value)) {
                row.put(target, recodeCase.toValue());
                return;
            }
        }
    }

    @Override
    public List<String> sourceVariables() {
        return Collections.singletonList(source);
    }

    @Override
    public String javaRule() {
        return "recode(" + source + " -> " + target + ")";
    }
}
