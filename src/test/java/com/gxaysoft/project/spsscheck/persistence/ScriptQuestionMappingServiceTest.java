package com.gxaysoft.project.spsscheck.persistence;

import com.gxaysoft.project.spsscheck.model.AnswerRecord;
import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import com.gxaysoft.project.spsscheck.model.RowContext;
import com.gxaysoft.project.spsscheck.io.AnswerPivot;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptQuestionMappingServiceTest {

    @Test
    void infersScriptTableIdFromSpsName() {
        assertEquals(3L, ScriptQuestionMappingService.inferTableIdFromScriptName("表2-1"));
        assertEquals(4L, ScriptQuestionMappingService.inferTableIdFromScriptName("表2-2.sps"));
        assertEquals(10L, ScriptQuestionMappingService.inferTableIdFromScriptName("表1-3"));
    }

    @Test
    void scriptMappingsPivotAnswersEvenWhenAnswerTableIdIsDifferent() {
        List<QuestionMapping> scriptMappings = Arrays.asList(
                new QuestionMapping(220014L, "BIRTH", "出生日期", 4L),
                new QuestionMapping(221001L, "EXAMINE", "体检时间", 4L)
        );
        Map<String, QuestionMapping> mappings = ScriptQuestionMappingService.toVariableMappings(scriptMappings);
        List<AnswerRecord> answers = Arrays.asList(
                new AnswerRecord(-1L, 1, "210911107430506", 220014L, 0L,
                        210911107430506L, "13/07/2024", 2L, 999L, "1", "2025", "0"),
                new AnswerRecord(-1L, 2, "210911107430506", 221001L, 0L,
                        210911107430506L, "15/10/2025", 2L, 999L, "1", "2025", "0")
        );

        RowContext row = AnswerPivot.pivot(answers, mappings).get(0);

        assertEquals("13/07/2024", row.get("BIRTH"));
        assertEquals("15/10/2025", row.get("EXAMINE"));
    }
}
