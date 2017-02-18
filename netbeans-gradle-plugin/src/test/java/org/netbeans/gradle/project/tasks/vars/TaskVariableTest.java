package org.netbeans.gradle.project.tasks.vars;

import org.junit.Test;
import org.netbeans.gradle.project.api.task.TaskVariable;

import static org.junit.Assert.*;

public class TaskVariableTest {

    private static TaskVariable create(String name) {
        return new TaskVariable(name);
    }

    private static void testValidVariable(String name) {
        assertTrue(TaskVariable.isValidVariableName(name));

        TaskVariable var = create(name);
        assertEquals(name, var.getVariableName());
        assertEquals("${" + name + "}", var.getScriptReplaceConstant());

        assertEquals(var, var);
        assertEquals(var, create(name));
    }

    private static void testInvalidVariable(String name) {
        assertFalse(TaskVariable.isValidVariableName(name));

        try {
            create(name);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testValidVariable() {
        testValidVariable(".");
        testValidVariable("_");
        testValidVariable("-");
        testValidVariable("0");
        testValidVariable("9");
        testValidVariable("a");
        testValidVariable("A");
        testValidVariable("z");
        testValidVariable("Z");
        testValidVariable("m");
        testValidVariable("K");
        testValidVariable(".-_0123456789ABCDEFGHIJKLMNOPQSTUVWXYZabcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void testInvalidVariable() {
        testInvalidVariable("");
        testInvalidVariable("$");
        testInvalidVariable("{");
        testInvalidVariable("}");
        testInvalidVariable("afegeg$efeffe");
    }

    private static void assertNotEquals(Object value1, Object value2) {
        if (value1 != null) {
            assertFalse(value1.equals(value2));
        }

        if (value2 != null) {
            assertFalse(value2.equals(value1));
        }
    }

    @Test
    public void testNotEquals() {
        assertNotEquals(create("ABC"), null);
        assertNotEquals(create("ABC"), create("abc"));
        assertNotEquals(create("ABC"), "ABC");
    }
}
