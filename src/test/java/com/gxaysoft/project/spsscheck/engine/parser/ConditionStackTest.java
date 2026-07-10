package com.gxaysoft.project.spsscheck.engine.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConditionStackTest {

    @Test public void testEmptyStackReturnsNull() {
        ConditionStack stack = new ConditionStack();
        assertNull(stack.buildCondition());
        assertEquals(0, stack.depth());
    }

    @Test public void testSinglePush() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("A = 1");
        assertEquals("A = 1", stack.buildCondition());
        assertEquals(1, stack.depth());
    }

    @Test public void testDepthTwo() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("grade <> 53");
        stack.pushDoIf("ZJTYPE = 1");
        assertEquals("grade <> 53 AND ZJTYPE = 1", stack.buildCondition());
        assertEquals(2, stack.depth());
    }

    @Test public void testDepthThree() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("C1");
        stack.pushDoIf("C2");
        stack.pushDoIf("C3");
        assertEquals("C1 AND C2 AND C3", stack.buildCondition());
        assertEquals(3, stack.depth());
    }

    @Test public void testElseNegatesTop() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("A = 1");
        stack.applyElse();
        assertEquals("NOT(A = 1)", stack.buildCondition());
    }

    @Test public void testElseInNestedContext() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("C1");
        stack.pushDoIf("C2");
        stack.applyElse();
        assertEquals("C1 AND NOT(C2)", stack.buildCondition());
    }

    @Test public void testPopEndIf() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("C1");
        stack.pushDoIf("C2");
        stack.popEndIf();
        assertEquals("C1", stack.buildCondition());
        assertEquals(1, stack.depth());
        stack.popEndIf();
        assertNull(stack.buildCondition());
        assertEquals(0, stack.depth());
    }

    @Test public void testEndToEndSimulatedParse() {
        ConditionStack stack = new ConditionStack();
        stack.pushDoIf("L=1");
        stack.pushDoIf("A");
        assertEquals("L=1 AND A", stack.buildCondition());
        stack.applyElse();
        assertEquals("L=1 AND NOT(A)", stack.buildCondition());
        stack.popEndIf();
        stack.popEndIf();
        assertNull(stack.buildCondition());
    }
}
