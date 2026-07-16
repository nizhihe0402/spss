package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 血压块特殊处理：收缩压/舒张压/压差三个独立检查变量，
 * 即使在同一个 DO IF 块内穿插赋值，也应各自独立成规则（各 1 条），
 * 不被其他变量的代码段阻断归组。
 */
public class RuleParserBloodPressureTest {

    /** 模拟表2-1 行1025-1043 的血压块：DO IF 内穿插三条 RECODE INTO + 三条 IF + 三条 self-RECODE。 */
    private static final String BP_BLOCK =
            "COMPUTE BPC = Q81 - Q82.\n" +
            "EXECUTE.\n" +
            "\n" +
            "DO IF(Q81 ~= 999 and Q82 ~= 999).\n" +
            "RECODE Q81 (Lowest thru 59=1) (271 thru Highest=1) (ELSE=0) INTO 收缩压可疑.\n" +
            "RECODE Q82 (151 thru Highest=1) (ELSE=0) INTO 舒张压可疑.\n" +
            "RECODE BPC (Lowest thru 10=1) (300 thru Highest=1) (ELSE=0) INTO 压差可疑.\n" +
            "END IF.\n" +
            "EXECUTE.\n" +
            "\n" +
            "IF(Q81=999 AND Q82=999) 收缩压可疑=0.\n" +
            "IF(Q81=999 AND Q82=999) 舒张压可疑=0.\n" +
            "IF(Q81=999 AND Q82=999) 压差可疑=0.\n" +
            "EXECUTE.\n" +
            "\n" +
            "RECODE 收缩压可疑 (1=1) (0=0) (MISSING=1).\n" +
            "RECODE 舒张压可疑 (1=1) (0=0) (MISSING=1).\n" +
            "RECODE 压差可疑 (1=1) (0=0) (MISSING=1).\n" +
            "EXECUTE.\n";

    @Test
    public void threeBloodPressureVariablesEachGetOneRule() {
        ParsedScript parsed = SpssParser.parse(BP_BLOCK);

        assertEquals(1, countRules(parsed, "收缩压可疑"), "收缩压可疑应恰好 1 条规则: " + describe(parsed));
        assertEquals(1, countRules(parsed, "舒张压可疑"), "舒张压可疑应恰好 1 条规则: " + describe(parsed));
        assertEquals(1, countRules(parsed, "压差可疑"), "压差可疑应恰好 1 条规则: " + describe(parsed));

        // BPC 是中间计算变量，可以独立成规则（没有同行消费）
    }

    /** 每条规则包含完整的三步：RECODE INTO + IF + self-RECODE。 */
    @Test
    public void eachRuleHasThreeSteps() {
        ParsedScript parsed = SpssParser.parse(BP_BLOCK);

        for (String target : new String[]{"收缩压可疑", "舒张压可疑", "压差可疑"}) {
            Rule rule = findRule(parsed, target);
            assertNotNull(rule, target + " 规则缺失");
            assertEquals(3, rule.getSteps().size(),
                    target + " 应有 3 步 (RECODE INTO + IF + self-RECODE): " + stepsSummary(rule));
        }
    }

    private static int countRules(ParsedScript script, String target) {
        int n = 0;
        for (Rule r : script.getRules()) {
            if (target.equals(r.getTarget())) n++;
        }
        return n;
    }

    private static Rule findRule(ParsedScript script, String target) {
        for (Rule r : script.getRules()) {
            if (target.equals(r.getTarget())) return r;
        }
        return null;
    }

    private static String describe(ParsedScript script) {
        StringBuilder sb = new StringBuilder("rules=[");
        for (Rule r : script.getRules()) sb.append(r.getTarget()).append(",");
        return sb.append("]").toString();
    }

    private static String stepsSummary(Rule rule) {
        StringBuilder sb = new StringBuilder("[");
        for (Step s : rule.getSteps()) {
            String preview = s.javaPreview();
            if (preview.length() > 40) preview = preview.substring(0, 40) + "...";
            sb.append(preview).append("; ");
        }
        return sb.append("]").toString();
    }
}
