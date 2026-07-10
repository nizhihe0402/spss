package com.gxaysoft.project.spsscheck.validation;

import com.gxaysoft.project.spsscheck.model.QuestionMapping;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceQuestionMappingFormatterTest {

    @Test
    void formatsEachSourceVariableWithQuestionIdAndKeepsMissingVariablesVisible() {
        Map<String, QuestionMapping> mappings = new LinkedHashMap<String, QuestionMapping>();
        mappings.put("ID1", new QuestionMapping(101L, "ID1", "ID", 21L));
        mappings.put("BIRTH", new QuestionMapping(102L, "BIRTH", "生日", 21L));

        String formatted = SourceQuestionMappingFormatter.format("ID1, BIRTH, age2", mappings);

        assertEquals("ID1 -> 101\nBIRTH -> 102\nage2 -> -", formatted);
    }
}
