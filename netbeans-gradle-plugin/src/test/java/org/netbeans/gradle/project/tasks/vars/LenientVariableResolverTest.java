package org.netbeans.gradle.project.tasks.vars;

import org.junit.Test;
import org.netbeans.gradle.project.api.task.TaskVariable;

import static org.junit.Assert.*;

public final class LenientVariableResolverTest {
    private DisplayedTaskVariable createVar(String name) {
        return new DisplayedTaskVariable(new TaskVariable(name), name, VariableTypeDescription.DEFAULT_TYPE);
    }

    private DisplayedTaskVariable createVar(String name, String displayName) {
        return new DisplayedTaskVariable(new TaskVariable(name), displayName, VariableTypeDescription.DEFAULT_TYPE);
    }

    private DisplayedTaskVariable createVar(String name, String displayName, String type, String typeArguments) {
        return new DisplayedTaskVariable(
                new TaskVariable(name),
                displayName,
                new VariableTypeDescription(type, typeArguments));
    }

    @Test
    public void testComplete() {
        DisplayedTaskVariable expected = createVar("varName", "Display Name", "varType", "varArgs");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName [ varType : varArgs ] : Display Name ");

        assertEquals(expected, parsed);
        assertEquals("${varName[vartype:varArgs]:Display Name}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testCompleteWithoutDisplayNameSeparator() {
        DisplayedTaskVariable expected = createVar("varName", "Display Name", "varType", "varArgs");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName [ varType : varArgs ]Display Name ");

        assertEquals(expected, parsed);
        assertEquals("${varName[vartype:varArgs]:Display Name}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testWithoutDisplayName() {
        DisplayedTaskVariable expected = createVar("varName", "varName", "varType", "varArgs");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName [ varType : varArgs ]");

        assertEquals(expected, parsed);
        assertEquals("${varName[vartype:varArgs]}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testWithoutTypeArguments() {
        DisplayedTaskVariable expected = createVar("varName", "Display Name", "varType", "");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName [varType] : Display Name ");

        assertEquals(expected, parsed);
        assertEquals("${varName[vartype]:Display Name}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testWithoutType() {
        DisplayedTaskVariable expected = createVar("varName", "Display Name");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName:Display Name");

        assertEquals(expected, parsed);
        assertEquals("${varName:Display Name}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testWithoutTypeDisplayNameContainsBracket() {
        DisplayedTaskVariable expected = createVar("varName", "Display Name[1]");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName:Display Name[1]");

        assertEquals(expected, parsed);
        assertEquals("${varName:Display Name[1]}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testVarNameOnly() {
        DisplayedTaskVariable expected = createVar("varName");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName ");

        assertEquals(expected, parsed);
        assertEquals("${varName}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testWithoutDisplayNameWithSpecialChars() {
        DisplayedTaskVariable expected = createVar("varName", "varName", "varType", "var\\\\Args\\[0\\]");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName [ varType : var\\\\Args\\[0\\] ]");

        assertEquals(expected, parsed);
        assertEquals("${varName[vartype:var\\\\Args\\[0\\]]}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testWithoutTypeWithSpecialChars() {
        DisplayedTaskVariable expected = createVar("varName", "Display\\Name[0]");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName:Display\\\\Name\\[0\\]");

        assertEquals(expected, parsed);
        assertEquals("${varName:Display\\\\Name[0]}", parsed.getScriptReplaceConstant());
    }

    @Test
    public void testCompleteWithSpecialChars() {
        DisplayedTaskVariable expected = createVar("varName", "Display{Name}", "varType", "var\\\\\\[Args\\]");
        DisplayedTaskVariable parsed = LenientVariableResolver.tryParseTaskVariable(
                " varName [ varType : var\\\\\\[Args\\] ] : Display\\{Name\\} ");

        assertEquals(expected, parsed);
        assertEquals("${varName[vartype:var\\\\\\[Args\\]]:Display{Name\\}}", parsed.getScriptReplaceConstant());
    }
}
