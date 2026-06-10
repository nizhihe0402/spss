package com.gxaysoft.project.spsscheck.v1.model;
import com.gxaysoft.project.spsscheck.model.RowContext;

import java.util.List;

public interface RuleStep {
    void execute(RowContext row);

    List<String> sourceVariables();

    String javaRule();
}
