package com.gxaysoft.project.spsscheck.v1.parser;

import com.gxaysoft.project.spsscheck.io.PrototypeFileReaders;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.v1.executor.RuleEngine;
import com.gxaysoft.project.spsscheck.v1.model.SpssCheckRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpssRuleParserRecodeIntoTest {

    @Test
    void table21CityMissingRuleUsesCitySourceOnly() throws Exception {
        String sps = PrototypeFileReaders.readSpssText(Paths.get("docs/sources/sps/表2-1.sps"));
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        SpssCheckRule cityMissing = null;
        int sortNo = 0;
        int citySortNo = -1;
        for (SpssCheckRule rule : rules) {
            sortNo++;
            if ("地市编码缺失".equals(rule.getTarget())) {
                cityMissing = rule;
                citySortNo = sortNo;
                break;
            }
        }

        assertEquals(17, citySortNo);
        assertEquals("地市编码缺失", cityMissing.getTarget());
        assertEquals(1, cityMissing.getSourceVariables().size());
        assertEquals("CITY", cityMissing.getSourceVariables().get(0));
        assertTrue(cityMissing.getSpssSource().startsWith("RECODE city"));

        RowContext row = new RowContext("210411106010101");
        row.put("city", "04");
        RuleEngine.execute(java.util.Collections.singletonList(row), java.util.Collections.singletonList(cityMissing));
        assertEquals(0, row.getFlag("地市编码缺失"));
    }

    @Test
    void leadingZeroGradeMatchesNumericRange() {
        String sps = "RECODE grade (1 thru 6=0)(11 thru 14=0)(SYSMIS=1)(ELSE=1) INTO grade_error.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        RowContext row = new RowContext("student-1");
        row.put("grade", "01");
        RuleEngine.execute(java.util.Collections.singletonList(row), rules);

        assertEquals(0, row.getFlag("grade_error"));
    }

    @Test
    void leadingZeroGradeMatchesDoIfConditionForAgeRange() {
        String sps = ""
                + "DO IF (grade = 6).\n"
                + "RECODE age2 (9 thru 14.5=0) (ELSE=1) INTO age_suspicious.\n"
                + "END IF.\n"
                + "EXECUTE.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        RowContext row = new RowContext("student-1");
        row.put("grade", "06");
        row.put("age2", "9.04");
        RuleEngine.execute(java.util.Collections.singletonList(row), rules);

        assertEquals(0, row.getFlag("age_suspicious"));
        assertEquals("0", String.valueOf(row.get("age_suspicious")));
    }

    @Test
    void ampersandConditionExecutesHeightAndWeightRangeRules() {
        String sps = ""
                + "RECODE Q6 (0=1)(MISSING=1)(999=1) INTO height_error.\n"
                + "DO IF (gender = 1 & age = 9).\n"
                + "RECODE Q6 (Lowest thru 116.1=1)(116.2 thru 165.0=0)(165.1 thru Highest=1) INTO height_error.\n"
                + "END IF.\n"
                + "EXECUTE.\n"
                + "RECODE Q7 (0=1)(MISSING=1)(999=1) INTO weight_error.\n"
                + "DO IF (gender = 1 & age = 9).\n"
                + "RECODE Q7 (Lowest thru 16.9=1)(17.0 thru 82.0=0)(82.1 thru Highest=1) INTO weight_error.\n"
                + "END IF.\n"
                + "EXECUTE.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        RowContext row = new RowContext("student-1");
        row.put("gender", "1");
        row.put("age", "9");
        row.put("Q6", "129");
        row.put("Q7", "27.6");
        RuleEngine.execute(java.util.Collections.singletonList(row), rules);

        assertEquals(0, row.getFlag("height_error"));
        assertEquals("0", String.valueOf(row.get("height_error")));
        assertEquals(0, row.getFlag("weight_error"));
        assertEquals("0", String.valueOf(row.get("weight_error")));
    }

    @Test
    void unmatchedRecodeIntoCheckRulePassesInsteadOfTreatingMissingTargetAsFailure() {
        String sps = ""
                + "RECODE VISIONR (0.1 thru 3.2 =1) (SYSMIS =1) INTO myopia_suspicious.\n"
                + "RECODE VISIONL (0.1 thru 3.2 =1) (SYSMIS =1) INTO myopia_suspicious.\n";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        RowContext row = new RowContext("student-1");
        row.put("VISIONR", "4");
        row.put("VISIONL", "4.1");
        RuleEngine.execute(java.util.Collections.singletonList(row), rules);

        assertEquals(0, row.getFlag("myopia_suspicious"));
        assertEquals("0", String.valueOf(row.get("myopia_suspicious")));
    }

    @Test
    void missingAndHighestRecodeCasesStillFailWhenMatched() {
        String sps = ""
                + "COMPUTE perm_dmft=Q54+Q55+Q56.\n"
                + "RECODE perm_dmft (32 thru Highest=1) INTO perm_suspicious.\n"
                + "RECODE Q54 (MISSING=1)(32 thru Highest=1) INTO perm_suspicious.\n"
                + "RECODE Q55 (MISSING=1)(32 thru Highest=1) INTO perm_suspicious.\n"
                + "RECODE Q56 (MISSING=1)(32 thru Highest=1) INTO perm_suspicious.\n";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        RowContext normal = new RowContext("normal");
        normal.put("Q54", "0");
        normal.put("Q55", "1");
        normal.put("Q56", "0");
        RuleEngine.execute(java.util.Collections.singletonList(normal), rules);
        assertEquals(0, normal.getFlag("perm_suspicious"));

        RowContext high = new RowContext("high");
        high.put("Q54", "32");
        high.put("Q55", "0");
        high.put("Q56", "0");
        RuleEngine.execute(java.util.Collections.singletonList(high), rules);
        assertEquals(1, high.getFlag("perm_suspicious"));

        RowContext missing = new RowContext("missing");
        missing.put("Q54", "");
        missing.put("Q55", "0");
        missing.put("Q56", "0");
        RuleEngine.execute(java.util.Collections.singletonList(missing), rules);
        assertEquals(1, missing.getFlag("perm_suspicious"));
    }

    @Test
    void computeExpressionKeepsDecimalLiteralBeforeStatementTerminator() {
        String sps = "COMPUTE age2=DATEDIFF(EXAMINE,BIRTH,\"days\")/365.25.\nEXECUTE.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        SpssCheckRule age2 = findRule(rules, "age2");

        assertNotNull(age2);
        assertEquals("DATEDIFF(EXAMINE,BIRTH,\"days\")/365.25", age2.getExpression());
        assertTrue(age2.getJavaRule().contains("/365.25"));
    }

    @Test
    void datediffComputesExamineMinusBirthAsPositiveAge() {
        String sps = "COMPUTE age2=DATEDIFF(EXAMINE,BIRTH,\"days\")/365.25.\nEXECUTE.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        RowContext row = new RowContext("student-1");
        row.put("EXAMINE", "28/09/2025");
        row.put("BIRTH", "13/09/2016");
        RuleEngine.execute(java.util.Collections.singletonList(row), rules);

        BigDecimal age2 = row.getDecimal("age2");
        assertNotNull(age2);
        assertTrue(age2.compareTo(new BigDecimal("9.03")) > 0);
        assertTrue(age2.compareTo(new BigDecimal("9.05")) < 0);
    }

    @Test
    void recodeElseCopyIsComputedButNotCheckFailure() {
        String sps = "RECODE SCHOOL_TYPE (4 thru 5=4) (ELSE=Copy) INTO STAGE.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        SpssCheckRule stage = null;
        for (SpssCheckRule rule : rules) {
            if ("STAGE".equals(rule.getTarget())) {
                stage = rule;
                break;
            }
        }

        assertNotNull(stage);
        assertFalse(stage.isCheckRule());

        RowContext row = new RowContext("student-1");
        row.put("SCHOOL_TYPE", "2");
        RuleEngine.execute(java.util.Collections.singletonList(row), rules);

        assertEquals("2", String.valueOf(row.get("STAGE")));
        assertEquals(0, row.getFlag("STAGE"));
    }

    @Test
    void computeSourceForRecodeIntoIsNotCheckRule() {
        String sps = ""
                + "COMPUTE 乳龋失补=Q51+Q52+Q53.\n"
                + "RECODE 乳龋失补(21 thru Highest=1) INTO 乳龋可疑.";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        SpssCheckRule computed = findRule(rules, "乳龋失补");
        SpssCheckRule check = findRule(rules, "乳龋可疑");

        assertNotNull(computed);
        assertFalse(computed.isCheckRule());
        assertNotNull(check);
        assertTrue(check.isCheckRule());
    }

    @Test
    void computeFollowedBySelfRecodeRemainsCheckRule() {
        String sps = ""
                + "COMPUTE ID是否一致=ID1-ID3.\n"
                + "RECODE ID是否一致 (0=0) (SYSMIS=1) (ELSE=1).";
        List<SpssCheckRule> rules = SpssRuleParser.parseRules(sps);

        SpssCheckRule rule = findRule(rules, "ID是否一致");

        assertNotNull(rule);
        assertTrue(rule.isCheckRule());
    }

    private static SpssCheckRule findRule(List<SpssCheckRule> rules, String target) {
        for (SpssCheckRule rule : rules) {
            if (target.equals(rule.getTarget())) {
                return rule;
            }
        }
        return null;
    }
}
