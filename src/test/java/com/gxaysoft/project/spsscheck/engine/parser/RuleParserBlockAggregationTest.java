package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import com.gxaysoft.project.spsscheck.model.RowContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DO IF 块聚合：块内被消费的中间变量不再产生独立规则，
 * 而是并入「汇变量」规则作为按序执行的步骤（设计文档
 * docs/superpowers/specs/2026-07-15-doif-block-aggregation-design.md）。
 */
public class RuleParserBlockAggregationTest {

    /** 模拟表2-1 身份证出生日期异常块（含非标准 IF/ELSE/END IF 与中间变量链）。 */
    private static final String BIRTHDATE_BLOCK =
            "COMPUTE 出生日期异常 = $SYSMIS.\n" +
            "\n" +
            "DO IF (ZJTYPE = 1).\n" +
            "    IF (MISSING(SFZ) OR MISSING(BIRTH)) 出生日期异常 = 1.\n" +
            "    ELSE.\n" +
            "        COMPUTE SFZ_DATE = CHAR.SUBSTR(SFZ, 7, 8).\n" +
            "        COMPUTE ID_DATE = NUMBER(SFZ_DATE, F8.0).\n" +
            "        COMPUTE BIRTH_DATE = XDATE.YEAR(BIRTH) * 10000 + XDATE.MONTH(BIRTH) * 100 + XDATE.MDAY(BIRTH).\n" +
            "        IF (NOT MISSING(ID_DATE)) 出生日期异常 = (ID_DATE <> BIRTH_DATE).\n" +
            "    END IF.\n" +
            "END IF.\n" +
            "EXECUTE.\n";

    @Test
    public void aggregatesIntermediateVariablesIntoSinkRule() {
        ParsedScript parsed = SpssParser.parse(BIRTHDATE_BLOCK);

        List<Rule> sinkRules = rulesByTarget(parsed, "出生日期异常");
        assertEquals(1, sinkRules.size(), "出生日期异常 应恰好一条规则，实际: " + describe(parsed));
        assertTrue(rulesByTarget(parsed, "SFZ_DATE").isEmpty(), "SFZ_DATE 不应独立成规则");
        assertTrue(rulesByTarget(parsed, "ID_DATE").isEmpty(), "ID_DATE 不应独立成规则");
        assertTrue(rulesByTarget(parsed, "BIRTH_DATE").isEmpty(), "BIRTH_DATE 不应独立成规则");

        Rule rule = sinkRules.get(0);
        // 步骤按源码顺序：init($SYSMIS) → IF缺失=1 → SFZ_DATE → ID_DATE → BIRTH_DATE → IF比较
        List<String> stepTargets = new ArrayList<String>();
        for (Step s : rule.getSteps()) {
            stepTargets.add(s.getTarget());
        }
        int iSfzDate = stepTargets.indexOf("SFZ_DATE");
        int iIdDate = stepTargets.indexOf("ID_DATE");
        int iBirthDate = stepTargets.indexOf("BIRTH_DATE");
        assertTrue(iSfzDate >= 0 && iIdDate > iSfzDate && iBirthDate > iIdDate,
                "中间步骤应按源码顺序存在，实际: " + stepTargets);
        assertTrue(stepTargets.indexOf("出生日期异常") >= 0, "汇变量步骤缺失: " + stepTargets);

        // 源变量：外部输入保留，块内中间变量剔除
        assertTrue(rule.getSourceVariables().contains("SFZ"), "sources 应含 SFZ: " + rule.getSourceVariables());
        assertTrue(rule.getSourceVariables().contains("BIRTH"), "sources 应含 BIRTH: " + rule.getSourceVariables());
        assertFalse(rule.getSourceVariables().contains("SFZ_DATE"), "sources 不应含中间变量: " + rule.getSourceVariables());
        assertFalse(rule.getSourceVariables().contains("ID_DATE"), "sources 不应含中间变量: " + rule.getSourceVariables());
    }

    @Test
    public void mergedStepsCarryFullChainedConditions() {
        ParsedScript parsed = SpssParser.parse(BIRTHDATE_BLOCK);
        Rule rule = rulesByTarget(parsed, "出生日期异常").get(0);
        String sfzDateCond = null;
        for (Step s : rule.getSteps()) {
            if ("SFZ_DATE".equals(s.getTarget())) {
                sfzDateCond = s.getCondition();
            }
        }
        assertNotNull(sfzDateCond, "SFZ_DATE 步骤应存在");
        assertTrue(sfzDateCond.contains("ZJTYPE = 1"),
                "中间步骤条件应含外层 DO IF 条件，实际: " + sfzDateCond);
        assertTrue(sfzDateCond.contains("NOT(MISSING(SFZ)"),
                "中间步骤条件应含 ELSE 取反条件，实际: " + sfzDateCond);
    }

    /** 血压块形态：多汇且无中间变量 → 保持独立规则（与现状一致）。 */
    @Test
    public void multiSinkBlockWithoutIntermediatesStaysSeparate() {
        String script =
                "DO IF (Q81 ~= 999 AND Q82 ~= 999).\n" +
                "COMPUTE 收缩压可疑 = (Q81 > 200).\n" +
                "COMPUTE 舒张压可疑 = (Q82 > 140).\n" +
                "COMPUTE 压差可疑 = (Q81 - Q82 < 10).\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        assertEquals(1, rulesByTarget(parsed, "收缩压可疑").size(), describe(parsed));
        assertEquals(1, rulesByTarget(parsed, "舒张压可疑").size(), describe(parsed));
        assertEquals(1, rulesByTarget(parsed, "压差可疑").size(), describe(parsed));
    }

    /** 多汇共享中间变量 → 每个汇各一条规则，中间步骤复制进各自规则。 */
    @Test
    public void sharedIntermediateIsCopiedIntoEachSinkRule() {
        String script =
                "DO IF (GRADE = 1).\n" +
                "COMPUTE TMP_SUM = Q1 + Q2.\n" +
                "COMPUTE A可疑 = (TMP_SUM > 10).\n" +
                "COMPUTE B可疑 = (TMP_SUM < 2).\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        assertTrue(rulesByTarget(parsed, "TMP_SUM").isEmpty(), "共享中间变量不应独立成规则: " + describe(parsed));
        List<Rule> a = rulesByTarget(parsed, "A可疑");
        List<Rule> b = rulesByTarget(parsed, "B可疑");
        assertEquals(1, a.size(), describe(parsed));
        assertEquals(1, b.size(), describe(parsed));
        assertTrue(hasStepForTarget(a.get(0), "TMP_SUM"), "A可疑 应含 TMP_SUM 步骤");
        assertTrue(hasStepForTarget(b.get(0), "TMP_SUM"), "B可疑 应含 TMP_SUM 步骤");
    }

    /** 合并后的规则可正确执行：中间步骤先算，汇步骤后判。 */
    @Test
    public void mergedRuleExecutesStepsInOrder() {
        String script =
                "DO IF (T = 1).\n" +
                "COMPUTE M = Q1 + 1.\n" +
                "IF (M > 5) 异常 = 1.\n" +
                "END IF.\n" +
                "EXECUTE.\n";
        ParsedScript parsed = SpssParser.parse(script);
        Rule rule = rulesByTarget(parsed, "异常").get(0);
        assertTrue(hasStepForTarget(rule, "M"), "M 应为内部步骤");

        RowContext hit = new RowContext("s1");
        hit.put("T", new BigDecimal("1"));
        hit.put("Q1", new BigDecimal("10"));
        for (Step s : rule.getSteps()) {
            s.execute(hit);
        }
        assertEquals(0, new BigDecimal("1").compareTo(hit.getDecimal("异常")), "T=1,Q1=10 应判异常=1");

        RowContext missBranch = new RowContext("s2");
        missBranch.put("T", new BigDecimal("2"));
        missBranch.put("Q1", new BigDecimal("10"));
        for (Step s : rule.getSteps()) {
            s.execute(missBranch);
        }
        assertNull(missBranch.getDecimal("异常"), "T=2 不进入 DO IF，异常应保持缺失");

        RowContext small = new RowContext("s3");
        small.put("T", new BigDecimal("1"));
        small.put("Q1", new BigDecimal("1"));
        for (Step s : rule.getSteps()) {
            s.execute(small);
        }
        assertNull(small.getDecimal("异常"), "M=2 不满足 M>5，异常应保持缺失");
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

    private static boolean hasStepForTarget(Rule rule, String target) {
        for (Step s : rule.getSteps()) {
            if (target.equals(s.getTarget())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(ParsedScript script) {
        StringBuilder sb = new StringBuilder("rules=[");
        for (Rule r : script.getRules()) {
            sb.append(r.getTarget()).append(", ");
        }
        return sb.append("]").toString();
    }
}
