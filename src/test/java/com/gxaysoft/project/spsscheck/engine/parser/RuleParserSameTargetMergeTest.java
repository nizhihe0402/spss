package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同名目标相邻代码段合并：脚本作者迭代写法（同一检查变量被相邻的多个
 * DO IF 块先后计算，中间只隔标签/统计/输出语句）应合并为一条规则，
 * 步骤按源码顺序执行（后算覆盖前算，与 SPSS 逐行执行语义一致）。
 * 隔着其他目标变量计算段的同名段不合并。
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
    public void mergesIteratedSameTargetBlocksIntoOneRule() {
        ParsedScript parsed = SpssParser.parse(ITERATED_SCRIPT);
        List<Rule> rules = rulesByTarget(parsed, "出生日期异常");
        assertEquals(1, rules.size(), "迭代写法应合并为一条规则，实际: " + describe(parsed));
        assertTrue(rulesByTarget(parsed, "SFZ_DATE").isEmpty(), "中间变量不应独立成规则");

        // 第一版计算的步骤在前，第二版在后（含中间的 $SYSMIS 重置）
        Rule rule = rules.get(0);
        int firstVersion = -1, reInit = -1, secondVersion = -1;
        for (int i = 0; i < rule.getSteps().size(); i++) {
            Step s = rule.getSteps().get(i);
            String preview = s.javaPreview();
            if (preview.contains("NUMBER(CHAR.SUBSTR") && firstVersion < 0) firstVersion = i;
            if (preview.contains("$SYSMIS") && firstVersion >= 0 && reInit < 0) reInit = i;
            if ("SFZ_DATE".equals(s.getTarget())) secondVersion = i;
        }
        assertTrue(firstVersion >= 0, "缺少第一版计算步骤: " + rule.getSteps().size() + " steps");
        assertTrue(reInit > firstVersion, "两版之间应保留 $SYSMIS 重置步骤（顺序执行语义）");
        assertTrue(secondVersion > reInit, "第二版计算应在重置之后");
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
