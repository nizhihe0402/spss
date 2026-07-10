package com.gxaysoft.project.spsscheck.web;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleControllerV2CorrectionUpdateTest {

    @Test
    void normalizesCorrectionUpdatePayload() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("correctionEnabled", "true");
        body.put("correctionType", " NORMALIZE_REGION_CODE ");
        body.put("correctionVariables", " PROVINCE,CITY,COUNTY ");
        body.put("correctionSource", "规则源变量");
        body.put("correctionStrategy", "省市区编码非2位时取右2位参与ID3计算");
        body.put("correctionApplyStage", "BEFORE_RULE_EXECUTION");
        body.put("correctionWriteClean", 1);
        body.put("correctionWriteSource", false);
        body.put("correctionDescription", null);

        RuleControllerV2.CorrectionUpdate update = RuleControllerV2.CorrectionUpdate.from(body);

        assertEquals(1, update.enabled);
        assertEquals("NORMALIZE_REGION_CODE", update.type);
        assertEquals("PROVINCE,CITY,COUNTY", update.variables);
        assertEquals("规则源变量", update.source);
        assertEquals("省市区编码非2位时取右2位参与ID3计算", update.strategy);
        assertEquals("BEFORE_RULE_EXECUTION", update.applyStage);
        assertEquals(1, update.writeClean);
        assertEquals(0, update.writeSource);
        assertEquals("", update.description);
    }

    @Test
    void acceptsSnakeCaseNamesFromOlderClients() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("correction_enabled", "0");
        body.put("correction_type", "FILL_SCHOOL_CODE");
        body.put("correction_variables", "SCHOOL");
        body.put("correction_write_clean", "true");

        RuleControllerV2.CorrectionUpdate update = RuleControllerV2.CorrectionUpdate.from(body);

        assertEquals(0, update.enabled);
        assertEquals("FILL_SCHOOL_CODE", update.type);
        assertEquals("SCHOOL", update.variables);
        assertEquals(1, update.writeClean);
    }
}
