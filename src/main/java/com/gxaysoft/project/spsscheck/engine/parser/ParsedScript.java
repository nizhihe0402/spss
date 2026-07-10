package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.DatasetRule;
import com.gxaysoft.project.spsscheck.engine.model.OutputRule;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次 SPSS 解析的完整结果。
 */
public class ParsedScript {
    private final List<Rule> rules;
    private final List<DatasetRule> datasetRules;
    private final List<OutputRule> outputRules;
    private final List<String> unsupportedStatements;

    public ParsedScript() {
        this.rules = new ArrayList<>();
        this.datasetRules = new ArrayList<>();
        this.outputRules = new ArrayList<>();
        this.unsupportedStatements = new ArrayList<>();
    }

    public List<Rule> getRules() { return rules; }
    public List<DatasetRule> getDatasetRules() { return datasetRules; }
    public List<OutputRule> getOutputRules() { return outputRules; }
    public List<String> getUnsupportedStatements() { return unsupportedStatements; }
    public int totalRules() { return rules.size(); }
    public int totalDatasetRules() { return datasetRules.size(); }
    public int totalOutputRules() { return outputRules.size(); }
}
