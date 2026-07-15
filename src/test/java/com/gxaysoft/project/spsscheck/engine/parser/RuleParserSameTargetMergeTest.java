package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同名目标相邻段处理（用户决定 2026-07-15）：
 * 迭代写法的多版计算【各自保留为独立规则，不互相聚合】——每版含自己的
 * $SYSMIS 初始化步骤；init 规则按文本相邻并入紧随其后的实体规则
 * （修复 mergeInitDeclarations 遇连续 init 静默丢弃的问题）。
 */
public class RuleParserSameTargetMergeTest {

    /** 模拟表2-1 身份证出生日期异常：两版计算，中间只隔 VALUE LABELS/FREQUENCIES。 */
    private static final String ITERATED_SCRIPT =
            "COMPUTE 出生日期异常 = $SYSMIS.\n" +
            "\n" +
            "DO IF (ZJTYPE = 1).\n" +
            "    DO IF (MISSING(SFZ) OR MISSING(BIRTH)).\n" +
            "        COMPUTE 出生日期异常 = 1.\n" +
            "    ELSE.\n" +
            "        COMPUTE 出生日期异常 = (NUMBER(CHAR.SUBSTR(SFZ, 7, 8), F8.0) <> BIRTH).\n" +
            "    END IF.\n" +
            "END IF.\n" +
            "\n" +
            "VALUE LABELS 出生日期异常 0'正常' 1'异常'.\n" +
            "\n" +
            "FREQUENCIES VARIABLES=出生日期异常\n" +
            "/ORDER=ANALYSIS.\n" +
            "\n" +
            "COMPUTE 出生日期异常 = $SYSMIS.\n" +
            "\n" +
            "DO IF (ZJTYPE = 1).\n" +
            "    IF (MISSING(SFZ) OR MISSING(BIRTH)) 出生日期异常 = 1.\n" +
            "    ELSE.\n" +
            "        COMPUTE SFZ_DATE = CHAR.SUBSTR(SFZ, 7, 8).\n" +
            "        IF (NOT MISSING(SFZ_DATE)) 出生日期异常 = (SFZ_DATE <> BIRTH).\n" +
            "    END IF.\n" +
            "END IF.\n" +
            "EXECUTE.\n";

    @Test
    public void keepsIteratedVersionsAsSeparateRulesWithOwnInit() {
        ParsedScript parsed = SpssParser.parse(ITERATED_SCRIPT);
        List<Rule> rules = rulesByTarget(parsed, "出生日期异常");
        assertEquals(2, rules.size(), "迭代写法两版应各自保留为独立规则，实际: " + describe(parsed));
        assertTrue(rulesByTarget(parsed, "SFZ_DATE").isEmpty(), "中间变量不应独立成规则");

        // 每版都以自己的 $SYSMIS 初始化开头（init 不再被静默丢弃）
        for (Rule rule : rules) {
            assertFalse(rule.getSteps().isEmpty(), "版本规则应有步骤");
            Step first = rule.getSteps().get(0);
            assertTrue(first.javaPreview().contains("$SYSMIS"),
                    "每版首步应为 $SYSMIS 初始化，实际: " + first.javaPreview());
        }

        // 第一版含 NUMBER 直算比对，第二版含 SFZ_DATE 中间步骤
        boolean v1 = false, v2 = false;
        for (Rule rule : rules) {
            for (Step s : rule.getSteps()) {
                if (s.javaPreview().contains("NUMBER(CHAR.SUBSTR")) v1 = true;
                if ("SFZ_DATE".equals(s.getTarget())) v2 = true;
            }
        }
        assertTrue(v1, "第一版计算步骤缺失");
        assertTrue(v2, "第二版中间步骤缺失");
    }

    /** init($SYSMIS) 按文本相邻并入实体规则（证件位数异常形态）。 */
    @Test
    public void initIsAbsorbedIntoFollowingRuleByTextAdjacency() {
        String script =
                "COMPUTE 位数异常 = $SYSMIS.\n" +
                "DO IF (NOT MISSING(zjtype)).\n" +
                "   COMPUTE 位数异常 = 0.\n" +
                "   IF ((STRING(zjtype, F1)=\"1\" AND CHAR.LENGTH(SFZ) <> 18)) 位数异常=1.\n" +
                "END IF.\n" +
                "EXECUTE.\n" +
                "\n" +
                "COMPUTE 别的变量 = Q1 + 1.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        List<Rule> rules = rulesByTarget(parsed, "位数异常");
        assertEquals(1, rules.size(), "init 应并入实体规则而不是独立成规则: " + describe(parsed));
        assertTrue(rules.get(0).getSteps().get(0).javaPreview().contains("$SYSMIS"),
                "首步应为 $SYSMIS 初始化");
    }

    /** 隔着其他目标变量计算段的同名段：不合并。 */
    @Test
    public void doesNotMergeAcrossOtherTargetComputation() {
        String script =
                "DO IF (A = 1).\n" +
                "COMPUTE X标记 = Q1 + 1.\n" +
                "END IF.\n" +
                "EXECUTE.\n" +
                "\n" +
                "COMPUTE Y标记 = Q2 + 1.\n" +
                "EXECUTE.\n" +
                "\n" +
                "DO IF (A = 2).\n" +
                "COMPUTE X标记 = Q1 + 2.\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        assertEquals(2, rulesByTarget(parsed, "X标记").size(),
                "隔着 Y标记 计算段的两段 X标记 不应合并: " + describe(parsed));
    }

    private static List<Rule> rulesByTarget(ParsedScript script, String target) {
        List<Rule> result = new ArrayList<Rule>();
        for (Rule r : script.getRules()) {
            if (target.equals(r.getTarget())) {
                result.add(r);
            }
        }
        return result;
    }

    private static String describe(ParsedScript script) {
        StringBuilder sb = new StringBuilder("rules=[");
        for (Rule r : script.getRules()) {
            sb.append(r.getTarget()).append(", ");
        }
        return sb.append("]").toString();
    }
}
