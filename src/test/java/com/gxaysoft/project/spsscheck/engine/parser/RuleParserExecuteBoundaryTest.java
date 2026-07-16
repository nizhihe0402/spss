package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EXECUTE 优先段边界（用户规格 2026-07-15）：
 * 1. 深度 0 的 EXECUTE. 是第一优先级代码段边界；
 * 2. 同名相邻段按变量名归组（$SYSMIS 重初始化为分版信号）；
 * 3. DO IF ... END IF 内部出现的 EXECUTE 不作为边界（失效）。
 * 关键字边界列表保留为漏写 EXECUTE 时的兜底。
 */
public class RuleParserExecuteBoundaryTest {

    /** 规则1：段以 EXECUTE 结束，spssSource 不吞下一段内容。 */
    @Test
    public void executeTerminatesSegmentSource() {
        String script =
                "COMPUTE X标记 = A + 1.\n" +
                "EXECUTE.\n" +
                "\n" +
                "IF (X标记 > 1) Y标记 = 1.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        Rule rule = findRule(parsed, "X标记");
        assertNotNull(rule);
        assertFalse(rule.getSpssSource().contains("Y标记"),
                "X标记 的 spssSource 应止于 EXECUTE，不吞下一段: " + rule.getSpssSource());
    }

    /** 规则3：DO IF 内部的 EXECUTE 失效，不截断块内后续步骤。 */
    @Test
    public void executeInsideDoIfIsNotABoundary() {
        String script =
                "COMPUTE X标记 = A + 1.\n" +
                "RECODE X标记 (99=1).\n" +
                "EXECUTE.\n" +
                "\n" +
                "DO IF (A = 1).\n" +
                "COMPUTE Z标记 = B + 1.\n" +
                "EXECUTE.\n" +                    // ← DO IF 内，失效
                "RECODE Z标记 (99=1).\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        Rule rule = findRule(parsed, "Z标记");
        assertNotNull(rule);
        // 块内 EXECUTE 若被误认为边界，RECODE 步骤会丢失
        boolean hasRecodeStep = false;
        for (Step s : rule.getSteps()) {
            if (s.javaPreview().contains("RECODE")) {
                hasRecodeStep = true;
            }
        }
        assertTrue(hasRecodeStep,
                "DO IF 内的 EXECUTE 不应截断块内 RECODE 步骤，steps=" + previews(rule));
    }

    /** 规则2：同名 IF 赋值分布在多个 EXECUTE 段（证件号码缺失形态）→ 归为一条规则。 */
    @Test
    public void sameTargetIfAssignSegmentsGroupIntoOneRule() {
        String script =
                "DO IF (NOT MISSING(zjtype) AND zjtype = 1).\n" +
                "   IF (CHAR.LENGTH(SFZ) <> 18) 号码缺失 = 1.\n" +
                "END IF.\n" +
                "EXECUTE.\n" +
                "\n" +
                "DO IF (NOT MISSING(zjtype) AND zjtype = 2).\n" +
                "   IF (CHAR.LENGTH(MTP) <> 9) 号码缺失 = 1.\n" +
                "END IF.\n" +
                "EXECUTE.\n" +
                "\n" +
                "DO IF (NOT MISSING(zjtype) AND zjtype = 3).\n" +
                "   IF (CHAR.LENGTH(TRPMT) <> 8) 号码缺失 = 1.\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        List<Rule> rules = rulesByTarget(parsed, "号码缺失");
        assertEquals(1, rules.size(), "同名 IF 段应归组为一条规则: " + describe(parsed));
        assertEquals(3, rules.get(0).getSteps().size(),
                "三个 zjtype 分支都应成为步骤（原 alreadyHasCompute 会丢弃后两个）: "
                        + previews(rules.get(0)));
    }

    /** 规则2 分版信号回归：$SYSMIS 重初始化开启新版本，不与前版合并。 */
    @Test
    public void sysmisReInitStartsNewVersion() {
        String script =
                "COMPUTE V标记 = $SYSMIS.\n" +
                "DO IF (A = 1).\n" +
                "COMPUTE V标记 = B + 1.\n" +
                "END IF.\n" +
                "EXECUTE.\n" +
                "\n" +
                "COMPUTE V标记 = $SYSMIS.\n" +
                "DO IF (A = 1).\n" +
                "COMPUTE V标记 = B + 2.\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        assertEquals(2, rulesByTarget(parsed, "V标记").size(),
                "$SYSMIS 重初始化应分版，两版独立: " + describe(parsed));
    }

    private static Rule findRule(ParsedScript script, String target) {
        List<Rule> rules = rulesByTarget(script, target);
        return rules.isEmpty() ? null : rules.get(0);
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

    private static String previews(Rule rule) {
        StringBuilder sb = new StringBuilder("[");
        for (Step s : rule.getSteps()) {
            sb.append(s.javaPreview()).append("; ");
        }
        return sb.append("]").toString();
    }

    private static String describe(ParsedScript script) {
        StringBuilder sb = new StringBuilder("rules=[");
        for (Rule r : script.getRules()) {
            sb.append(r.getTarget()).append(", ");
        }
        return sb.append("]").toString();
    }
}
