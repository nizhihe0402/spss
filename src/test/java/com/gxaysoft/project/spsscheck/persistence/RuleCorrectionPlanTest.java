package com.gxaysoft.project.spsscheck.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleCorrectionPlanTest {

    @Test
    void detectsSchoolCodeFillForSchoolMissingRule() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "MISSING_CHECK", "学校编码缺失", "SCHOOL", "学校编码缺失");

        assertTrue(plan.enabled);
        assertEquals("FILL_SCHOOL_CODE", plan.type);
        assertEquals("SCHOOL", plan.variables);
        assertEquals("bus_student.school_id -> bus_school.school_code", plan.source);
        assertEquals("SCHOOL缺失时，从学生学校信息补学校编码", plan.strategy);
        assertEquals("BEFORE_RULE_EXECUTION", plan.applyStage);
        assertTrue(plan.writeClean);
        assertFalse(plan.writeSource);
    }

    @Test
    void detectsRegionNormalizationForIdentityRule() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "IDENTITY_CHECK", "ID是否一致", "ID1,PROVINCE,CITY,COUNTY,POINT,SCHOOL", "ID是否一致");

        assertTrue(plan.enabled);
        assertEquals("NORMALIZE_REGION_CODE", plan.type);
        assertEquals("PROVINCE,CITY,COUNTY", plan.variables);
        assertEquals("规则源变量", plan.source);
        assertEquals("省市区编码非2位时取右2位参与ID3计算", plan.strategy);
        assertEquals("BEFORE_RULE_EXECUTION", plan.applyStage);
        assertTrue(plan.writeClean);
        assertFalse(plan.writeSource);
    }

    @Test
    void detectsRegionNormalizationForParsedV1IdentityRuleWithOnlyIdSources() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "ROW_CHECK", "ID是否一致", "ID1,ID3", "校验ID是否一致：编码不匹配标记为1，一致标记为0");

        assertTrue(plan.enabled);
        assertEquals("NORMALIZE_REGION_CODE", plan.type);
        assertEquals("PROVINCE,CITY,COUNTY", plan.variables);
    }

    @Test
    void detectsDocumentInfoFillForZjtypeRule() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "DOCUMENT_CHECK", "ZJTYPE", "ZJTYPE", "证件类型缺失或异常");

        assertTrue(plan.enabled);
        assertEquals("FILL_DOCUMENT_INFO", plan.type);
        assertEquals("ZJTYPE", plan.variables);
        assertEquals("bus_student.id_type/card 或学生证件JSON", plan.source);
        assertEquals("证件信息缺失时，从学生信息补齐", plan.strategy);
        assertEquals("BEFORE_RULE_EXECUTION", plan.applyStage);
        assertTrue(plan.writeClean);
        assertFalse(plan.writeSource);
    }

    @Test
    void detectsDocumentInfoFillForCardRule() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "DOCUMENT_CHECK", "SFZ", "SFZ", "身份证号码缺失或异常");

        assertTrue(plan.enabled);
        assertEquals("FILL_DOCUMENT_INFO", plan.type);
        assertEquals("SFZ", plan.variables);
    }

    @Test
    void detectsAllDocumentNumberVariablesForDocumentCheck() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "DOCUMENT_CHECK", "DOC_LENGTH_CHECK", "ZJTYPE,SFZ,MTP,TRPMT,HZ", "document number length check");

        assertTrue(plan.enabled);
        assertEquals("FILL_DOCUMENT_INFO", plan.type);
        assertEquals("ZJTYPE,SFZ,MTP,TRPMT,HZ", plan.variables);
    }

    @Test
    void detectsAllDocumentNumberVariablesForCardMissingRuleEvenWhenSourcesOnlyContainSfz() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "DOCUMENT_CHECK", "证件号码缺失", "SFZ", "证件号码缺失");

        assertTrue(plan.enabled);
        assertEquals("FILL_DOCUMENT_INFO", plan.type);
        assertEquals("ZJTYPE,SFZ,MTP,TRPMT,HZ", plan.variables);
    }

    @Test
    void leavesUnrelatedRuleWithoutCorrection() {
        RuleCorrectionPlan plan = RuleCorrectionPlan.detect(
                "RANGE_CHECK", "BMI异常", "BMI", "BMI范围异常");

        assertFalse(plan.enabled);
        assertEquals("", plan.type);
        assertEquals("", plan.description);
    }
}
