package com.gxaysoft.project.spsscheck.engine.model;

import com.gxaysoft.project.spsscheck.model.RowContext;
import java.util.List;

public interface StepAction {
    void execute(RowContext row);
    String target();
    List<String> sourceVariables();
}
