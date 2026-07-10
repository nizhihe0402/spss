package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPSS 文本统一解析入口。产出 ParsedScript。
 * 替代 v1 SpssRuleParser + v1 parseOutputRules/parseDatasetRules。
 */
public final class SpssParser {
    private static final Logger log = LoggerFactory.getLogger(SpssParser.class);

    private SpssParser() {}

    public static ParsedScript parse(String spssText) {
        ParsedScript result = new ParsedScript();
        log.info("开始解析 SPSS 文本: {} 字符", spssText != null ? spssText.length() : 0);
        if (spssText == null || spssText.trim().isEmpty()) return result;

        // 1. 解析 COMPUTE/RECODE/IF 规则
        result.getRules().addAll(RuleParser.parseRules(spssText));

        // 2. 解析 SORT CASES + MATCH FILES 规则
        result.getDatasetRules().addAll(RuleParser.parseDatasetRules(spssText));

        // 3. 解析 SELECT IF + SAVE OUTFILE 输出规则
        result.getOutputRules().addAll(RuleParser.parseOutputRules(spssText));

        log.info("解析完成: rules={}, datasetRules={}, outputRules={}",
                result.totalRules(), result.totalDatasetRules(), result.totalOutputRules());
        return result;
    }
}
