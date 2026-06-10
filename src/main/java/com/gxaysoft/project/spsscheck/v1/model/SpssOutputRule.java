package com.gxaysoft.project.spsscheck.v1.model;
import com.gxaysoft.project.spsscheck.model.RowContext;

import com.gxaysoft.project.spsscheck.expression.ConditionExpression;

public class SpssOutputRule {
    private final String sheetName;
    private final String condition;
    private final String spssSource;
    private final String javaRule;

    public SpssOutputRule(String sheetName, String condition, String spssSource) {
        this.sheetName = sheetName;
        this.condition = condition;
        this.spssSource = spssSource;
        this.javaRule = "if evalCondition(\"" + condition + "\") then outputSheet(\"" + sheetName + "\")";
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getCondition() {
        return condition;
    }

    public String getSpssSource() {
        return spssSource;
    }

    public String getJavaRule() {
        return javaRule;
    }

    public boolean matches(RowContext row) {
        return new ConditionExpression(condition, row).eval();
    }
}
