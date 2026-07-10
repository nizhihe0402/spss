package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.model.RecodeCase;
import java.util.Collections;
import java.util.List;

public class RecodeAction implements StepAction {
    private final String source;
    private final String target;
    private final List<RecodeCase> cases;

    public RecodeAction(String source, String target, List<RecodeCase> cases) {
        this.source = source;
        this.target = target;
        this.cases = cases;
    }

    @Override
    public void execute(RowContext row) {
        Object value = row.get(source);
        for (RecodeCase recodeCase : cases) {
            if (recodeCase.matches(value)) {
                row.put(target, recodeCase.toValue(value));
                return;
            }
        }
    }

    @Override public String target() { return target; }
    public String getSource() { return source; }
    public List<RecodeCase> getCases() { return cases; }

    @Override
    public List<String> sourceVariables() {
        return Collections.singletonList(source);
    }
}
