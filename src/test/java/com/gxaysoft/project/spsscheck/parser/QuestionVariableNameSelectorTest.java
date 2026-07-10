package com.gxaysoft.project.spsscheck.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionVariableNameSelectorTest {

    @Test
    void usesExportContentAsSpssVariableAndIgnoresExportSort() {
        assertEquals("A121", QuestionVariableNameSelector.variableNameFromExportContent("A121"));
    }

    @Test
    void trimsExportContent() {
        assertEquals("A121", QuestionVariableNameSelector.variableNameFromExportContent(" A121 "));
    }

    @Test
    void returnsNullWhenExportContentDoesNotLookLikeAVariable() {
        assertNull(QuestionVariableNameSelector.variableNameFromExportContent("人员配备"));
    }
}
