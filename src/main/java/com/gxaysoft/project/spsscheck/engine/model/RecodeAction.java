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

    /** 是否含 ELSE 分支——有 ELSE 时 RECODE 必然写入目标变量（真正的覆盖）。 */
    public boolean alwaysWrites() {
        if (cases != null) {
            for (com.gxaysoft.project.spsscheck.model.RecodeCase c : cases) {
                if (c.isAlwaysWrites()) return true;
            }
        }
        return false;
    }

    @Override public String target() { return target; }
    public String getSource() { return source; }
    public List<com.gxaysoft.project.spsscheck.model.RecodeCase> getCases() { return cases; }

    @Override
    public List<String> sourceVariables() {
        return Collections.singletonList(source);
    }
}
