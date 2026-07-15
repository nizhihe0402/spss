package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.RuleType;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复现表2-1.sps 的 DO IF 条件泄漏问题：
 * 标准 DO IF ... ELSE ... END IF 块结束后，findActiveDoIfCondition 的条件栈
 * 残留了 DO IF 条件，导致后续无关的 COMPUTE（如 乳龋失补=Q51+Q52+Q53）
 * 被误加 SFZ 条件、混入 SFZ/GENDER 源变量并被 BlockClassifier 误分类为 DOCUMENT_CHECK。
 */
public class RuleParserDoIfConditionTest {

    /** 模拟表2-1.sps 结构：前有普通 IF 语句 + 标准 DO IF/ELSE/END IF 块，后跟无关 COMPUTE。
     *  分支内使用非常量表达式（常量赋值会被 isConstantAssignment 跳过，不生成 step）。 */
    private static final String SCRIPT_WITH_DO_IF_ELSE =
            "IF (MISSING(SFZ)) 证件缺失 = 1.\n" +
            "EXECUTE.\n" +
            "\n" +
            "DO IF (LENGTH(RTRIM(SFZ)) = 18 AND NOT MISSING(gender)).\n" +
            "    COMPUTE 身份证性别异常 = MOD(NUMBER(CHAR.SUBSTR(SFZ, 17, 1), F8.0), 2).\n" +
            "ELSE.\n" +
            "    COMPUTE 身份证性别异常 = GENDER_FLAG + 1.\n" +
            "END IF.\n" +
            "EXECUTE.\n" +
            "\n" +
            "COMPUTE 乳龋失补=Q51+Q52+Q53.\n" +
            "EXECUTE.\n";

    @Test
    public void computeAfterClosedDoIfElseBlockHasNoStaleCondition() {
        ParsedScript script = SpssParser.parse(SCRIPT_WITH_DO_IF_ELSE);
        Rule rule = findRuleByTarget(script, "乳龋失补");
        assertNotNull(rule, "应解析出 乳龋失补 规则");

        for (Step step : rule.getSteps()) {
            assertNull(step.getCondition(),
                    "DO IF 块已被 END IF 关闭，后续 COMPUTE 不应残留条件，实际: " + step.getCondition());
        }
        assertFalse(rule.getSourceVariables().contains("SFZ"),
                "源变量不应混入 SFZ，实际: " + rule.getSourceVariables());
        assertNotEquals(RuleType.DOCUMENT_CHECK, rule.getType(),
                "口腔检查规则不应被误分类为证件校验");
    }

    @Test
    public void elseBranchStepGetsNegatedDoIfCondition() {
        ParsedScript script = SpssParser.parse(SCRIPT_WITH_DO_IF_ELSE);

        // 两个分支的 COMPUTE 会各自解析为一条同名规则，遍历所有规则的所有步骤
        boolean sawNegated = false;
        for (Rule rule : script.getRules()) {
            if (!"身份证性别异常".equals(rule.getTarget())) {
                continue;
            }
            for (Step step : rule.getSteps()) {
                String cond = step.getCondition();
                if (cond != null && cond.startsWith("NOT(")) {
                    sawNegated = true;
                    assertTrue(cond.contains("LENGTH(RTRIM(SFZ)) = 18"),
                            "ELSE 分支应取反 DO IF 自身的条件，实际: " + cond);
                }
            }
        }
        assertTrue(sawNegated, "ELSE 分支的 COMPUTE 应携带 NOT(DO IF 条件)");
    }

    /** 表2-1.sps 第 267-295 行的非标准写法：IF ... ELSE ... END IF（配对多余 END IF），不应破坏后续解析。 */
    @Test
    public void nonStandardIfElseBlockDoesNotLeakCondition() {
        String script =
                "DO IF (ZJTYPE = 1).\n" +
                "    IF (MISSING(SFZ) OR MISSING(BIRTH)) 出生日期异常 = 1.\n" +
                "    ELSE.\n" +
                "        COMPUTE 出生日期异常 = 0.\n" +
                "    END IF.\n" +
                "END IF.\n" +
                "EXECUTE.\n" +
                "\n" +
                "COMPUTE 乳龋失补=Q51+Q52+Q53.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        Rule rule = findRuleByTarget(parsed, "乳龋失补");
        assertNotNull(rule, "应解析出 乳龋失补 规则");
        for (Step step : rule.getSteps()) {
            assertNull(step.getCondition(),
                    "非标准 IF/ELSE/END IF 块结束后不应残留条件，实际: " + step.getCondition());
        }
    }

    private static Rule findRuleByTarget(ParsedScript script, String target) {
        for (Rule rule : script.getRules()) {
            if (target.equals(rule.getTarget())) {
                return rule;
            }
        }
        return null;
    }
}
