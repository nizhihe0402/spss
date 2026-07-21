package com.gxaysoft.project.spsscheck.engine.parser;

import com.gxaysoft.project.spsscheck.engine.model.Rule;
import com.gxaysoft.project.spsscheck.engine.model.Step;
import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
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

        // 每版都含自己的 $SYSMIS 初始化（不再被静默丢弃；常量 1 可能排在前）
        for (Rule rule : rules) {
            assertFalse(rule.getSteps().isEmpty(), "版本规则应有步骤");
            boolean hasInit = false;
            for (Step s : rule.getSteps()) {
                if (s.javaPreview().contains("$SYSMIS")) { hasInit = true; break; }
            }
            assertTrue(hasInit, "每版应含 $SYSMIS 初始化步骤");
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
        assertEquals(1, rules.size(), "init 应并入实体规则: " + describe(parsed));
        boolean hasInit = false;
        for (Step s : rules.get(0).getSteps()) {
            if (s.javaPreview().contains("$SYSMIS")) hasInit = true;
        }
        assertTrue(hasInit, "应有 $SYSMIS 初始化步骤");
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

    /** DO IF...ELSE 块内 COMPUTE = 常量 注入后应在 $SYSMIS 之后，不在其前。
     *  否则无条件 $SYSMIS 步骤会覆盖常量条件赋值的结果。 */
    @Test
    public void constantInitInsideDoIfPlacedAfterSysmisInit() {
        // 单版本 DO IF...ELSE，IF 分支是常量 COMPUTE = 1
        String script =
                "COMPUTE 身份证出生日期异常 = $SYSMIS.\n" +
                "\n" +
                "DO IF ( ZJTYPE = 1).\n" +
                "    DO IF (MISSING(SFZ) OR MISSING(BIRTH) OR LENGTH(RTRIM(SFZ)) < 14).\n" +
                "        COMPUTE 身份证出生日期异常 = 1.\n" +
                "    ELSE.\n" +
                "        COMPUTE 身份证出生日期异常 = (NUMBER(CHAR.SUBSTR(SFZ, 7, 8), F8.0) <>\n" +
                "                                 (XDATE.YEAR(BIRTH)*10000 +\n" +
                "                                  XDATE.MONTH(BIRTH)*100 +\n" +
                "                                  XDATE.MDAY(BIRTH))).\n" +
                "    END IF.\n" +
                "END IF.\n";
        ParsedScript parsed = SpssParser.parse(script);
        List<Rule> rules = rulesByTarget(parsed, "身份证出生日期异常");
        assertEquals(1, rules.size(), "应是一条规则: " + describe(parsed));

        Rule rule = rules.get(0);
        List<Step> steps = rule.getSteps();
        assertEquals(3, steps.size(),
                "应有 3 步: $SYSMIS 初始化 → IF 分支 COMPUTE=1 → ELSE 分支 COMPUTE=formula, 实际: "
                + steps.stream().map(Step::javaPreview).reduce((a,b)->a+"\n"+b).orElse(""));

        // Step 1 应为无条件 $SYSMIS 初始化
        assertNull(steps.get(0).getCondition(), "Step 1 应为无条件 $SYSMIS");
        assertTrue(steps.get(0).javaPreview().contains("$SYSMIS"),
                "Step 1 应为 $SYSMIS 初始化");

        // Step 2 应为条件 COMPUTE = 1 (IF 分支)
        assertNotNull(steps.get(1).getCondition(), "Step 2 应有 DO IF 条件");
        assertTrue(steps.get(1).getCondition().contains("ZJTYPE"),
                "Step 2 条件应含 ZJTYPE");
        assertTrue(steps.get(1).javaPreview().contains("= 1") || steps.get(1).javaPreview().contains("=1"),
                "Step 2 应为 COMPUTE = 1");

        // Step 3 应为条件 COMPUTE = formula (ELSE 分支)
        assertNotNull(steps.get(2).getCondition(), "Step 3 应有 DO IF 条件");
        assertTrue(steps.get(2).javaPreview().contains("NUMBER") || steps.get(2).javaPreview().contains("CHAR.SUBSTR"),
                "Step 3 应为公式 COMPUTE");

        // spssSource 应包含完整源码（含 ELSE 和 COMPUTE = 1）
        String source = rule.getSpssSource();
        assertNotNull(source, "spssSource 不应为空");
        assertTrue(source.contains("ELSE"), "spssSource 应包含 ELSE, 实际: " + source);
        assertTrue(source.contains("身份证出生日期异常 = 1"),
                "spssSource 应包含 COMPUTE = 1, 实际: " + source);
    }

    /** 两版迭代（R005 DO IF+COMPUTE 版 / R006 DO IF+IF-assign+中间变量版）
     *  各自保留为独立规则，R006 的 spssSource 应显示 R006 代码而非 R005。 */
    @Test
    public void twoVersionsStaySeparateWithCorrectSpssSource() {
        // R005 版: DO IF...DO IF...COMPUTE...ELSE...COMPUTE
        // R006 版: DO IF...IF-assign...ELSE...STRING...COMPUTE...IF...ELSE...END IF
        String script =
                // ── R005: 第一版 ──
                "COMPUTE 异常标志 = $SYSMIS.\n" +
                "\n" +
                "DO IF ( ZJTYPE = 1).\n" +
                "    DO IF (MISSING(SFZ) OR MISSING(BIRTH) OR LENGTH(RTRIM(SFZ)) < 14).\n" +
                "        COMPUTE 异常标志 = 1.\n" +
                "    ELSE.\n" +
                "        COMPUTE 异常标志 = (NUMBER(CHAR.SUBSTR(SFZ, 7, 8), F8.0) <> BIRTH).\n" +
                "    END IF.\n" +
                "END IF.\n" +
                "\n" +
                "VALUE LABELS 异常标志 0'正常' 1'异常'.\n" +
                "FREQUENCIES VARIABLES=异常标志 /ORDER=ANALYSIS.\n" +
                "\n" +
                // ── R006: 第二版（IF-assign + 中间变量）──
                "COMPUTE 异常标志 = $SYSMIS.\n" +
                "\n" +
                "DO IF (ZJTYPE = 1).\n" +
                "    IF (MISSING(SFZ) OR MISSING(BIRTH)) 异常标志 = 1.\n" +
                "    ELSE.\n" +
                "        STRING SFZ_DATE (A8).\n" +
                "        COMPUTE SFZ_DATE = CHAR.SUBSTR(SFZ, 7, 8).\n" +
                "        COMPUTE BIRTH_DATE = XDATE.YEAR(BIRTH)*10000.\n" +
                "        IF (NOT MISSING(SFZ_DATE)) 异常标志 = (SFZ_DATE <> BIRTH_DATE).\n" +
                "        ELSE 异常标志 = 1.\n" +
                "    END IF.\n" +
                "END IF.\n" +
                "EXECUTE.\n";

        ParsedScript parsed = SpssParser.parse(script);
        List<Rule> rules = rulesByTarget(parsed, "异常标志");
        assertEquals(2, rules.size(),
                "两版应各自保留为独立规则: " + describe(parsed));

        // 按源码顺序排列
        rules.sort((a, b) -> {
            String sa = a.getSpssSource();
            String sb = b.getSpssSource();
            return Integer.compare(
                    script.indexOf(sa != null && !sa.isEmpty() ? sa.trim().split("\\r?\\n")[0] : ""),
                    script.indexOf(sb != null && !sb.isEmpty() ? sb.trim().split("\\r?\\n")[0] : ""));
        });

        Rule r005 = rules.get(0);
        Rule r006 = rules.get(1);

        // ── R005 断言 ──
        String src005 = r005.getSpssSource();
        assertNotNull(src005, "R005 spssSource 不应为空");
        assertTrue(src005.contains("NUMBER(CHAR.SUBSTR"),
                "R005 应包含 NUMBER 公式, 实际: " + src005);
        assertTrue(src005.contains("ELSE"),
                "R005 应包含 ELSE");

        // ── R006 断言 ──
        String src006 = r006.getSpssSource();
        assertNotNull(src006, "R006 spssSource 不应为空");
        assertTrue(src006.contains("SFZ_DATE"),
                "R006 应包含中间变量 SFZ_DATE, 实际: " + src006);
        assertTrue(src006.contains("BIRTH_DATE"),
                "R006 应包含中间变量 BIRTH_DATE, 实际: " + src006);
        assertTrue(src006.contains("NOT MISSING(SFZ_DATE)"),
                "R006 应包含条件 IF(NOT MISSING(SFZ_DATE)), 实际: " + src006);
        // R006 不应包含 R005 的 NUMBER 公式
        assertFalse(src006.contains("NUMBER(CHAR.SUBSTR"),
                "R006 spssSource 不应包含 R005 的 NUMBER 公式, 实际: " + src006);

        // ── R006 步骤断言 ──
        assertNotNull(r006.getSteps());
        assertTrue(r006.getSteps().size() >= 4,
                "R006 至少应有 $SYSMIS→IF-assign→SFZ_DATE→BIRTH_DATE→IF-assign, 实际 "
                + r006.getSteps().size() + " 步: "
                + r006.getSteps().stream().map(Step::javaPreview).reduce((a,b)->a+"\n"+b).orElse(""));

        // 应有中间变量步骤
        boolean hasSfzDate = false, hasBirthDate = false, hasIfAssign = false;
        for (Step s : r006.getSteps()) {
            String jp = s.javaPreview();
            if (jp.contains("SFZ_DATE")) hasSfzDate = true;
            if (jp.contains("BIRTH_DATE")) hasBirthDate = true;
            if (jp.contains("NOT MISSING")) hasIfAssign = true;
        }
        assertTrue(hasSfzDate, "R006 应有 SFZ_DATE 步骤");
        assertTrue(hasBirthDate, "R006 应有 BIRTH_DATE 步骤");
        assertTrue(hasIfAssign, "R006 应有 IF(NOT MISSING) 步骤");

        // spssSource 应存在且不含 NUMBER 公式
    }

    /** 表2-1 实际文件：R005/R006 两版 身份证出生日期异常 各自独立且 spssSource 正确。 */
    @Test
    public void realFileR005AndR006StaySeparate() throws Exception {
        String spsText = PrototypeFileReaders.readSpssText(
                Paths.get("docs/sources/sps/表2-1.sps"));
        ParsedScript parsed = SpssParser.parse(spsText);
        List<Rule> rules = rulesByTarget(parsed, "身份证出生日期异常");

        // 迭代写法两版各自保留
        assertEquals(2, rules.size(),
                "表2-1 身份证出生日期异常应有 2 条规则, 实际 "
                + rules.size() + ": " + describe(parsed));

        // R005（第一版）：含 NUMBER 公式
        // R006（第二版）：含 SFZ_DATE 中间变量
        boolean hasR005 = false, hasR006 = false;
        for (Rule r : rules) {
            String src = r.getSpssSource();
            assertNotNull(src, "spssSource 不应为空: " + r.getTarget());
            if (src.contains("NUMBER(CHAR.SUBSTR")) {
                hasR005 = true;
                assertTrue(src.contains("ELSE"), "R005 应包含 ELSE");
                // R005 不应包含 R006 的中间变量
                assertFalse(src.contains("SFZ_DATE"),
                        "R005 不应含 SFZ_DATE, 实际: " + src.substring(0, Math.min(200, src.length())));
            }
            if (src.contains("SFZ_DATE")) {
                hasR006 = true;
                // R006 不应包含 R005 的 NUMBER 公式
                assertFalse(src.contains("NUMBER(CHAR.SUBSTR"),
                        "R006 spssSource 不应含 R005 的 NUMBER 公式: " + src.substring(0, Math.min(200, src.length())));
                // R006 应有中间变量
                assertTrue(src.contains("ID_DATE"), "R006 应含 ID_DATE");
                // R006 应有 7 步: $SYSMIS → IF-assign(基础检查) → SFZ_DATE → ID_DATE
                // → BIRTH_DATE → IF(NOT MISSING(ID_DATE)) → ELSE 赋值(转换失败=1)
                assertEquals(7, r.getSteps().size(),
                        "R006 应有 7 步, 实际 " + r.getSteps().size() + ": "
                        + r.getSteps().stream().map(Step::javaPreview).reduce((a,b)->a+"\n"+b).orElse(""));
                // 最后一步应为 ELSE 转换失败 case，含 IF(MISSING(ID_DATE))
                Step lastStep = r.getSteps().get(r.getSteps().size() - 1);
                assertTrue(lastStep.javaPreview().contains("MISSING(ID_DATE)"),
                        "最后一步应含 IF(MISSING(ID_DATE)), 实际: " + lastStep.javaPreview());
                assertTrue(lastStep.javaPreview().contains("= 1"),
                        "最后一步应为赋值 = 1, 实际: " + lastStep.javaPreview());
            }
        }
        assertTrue(hasR005, "应找到 R005 规则（含 NUMBER 公式）");
        assertTrue(hasR006, "应找到 R006 规则（含 SFZ_DATE）");
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
