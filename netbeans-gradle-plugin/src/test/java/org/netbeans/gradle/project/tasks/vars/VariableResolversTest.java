package org.netbeans.gradle.project.tasks.vars;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

import static org.junit.Assert.*;

public final class VariableResolversTest {
    private static TaskVariableMap variableMap(Map<DisplayedTaskVariable, String> map) {
        return DisplayedTaskVariable.variableMap(map);
    }

    private DisplayedTaskVariable createVar(String name) {
        return new DisplayedTaskVariable(new TaskVariable(name), name, VariableTypeDescription.DEFAULT_TYPE);
    }

    private DisplayedTaskVariable createVar(String name, String displayName) {
        return new DisplayedTaskVariable(new TaskVariable(name), displayName, VariableTypeDescription.DEFAULT_TYPE);
    }

    private static String replaceVars(
            String str,
            TaskVariableMap varReplaceMap) {
        return VariableResolvers.getDefault().replaceVars(str, varReplaceMap);
    }

    public static String replaceVars(
            String str,
            TaskVariableMap varReplaceMap,
            Collection<? super DisplayedTaskVariable> collectedVariables) {
        return VariableResolvers.getDefault().replaceVars(str, varReplaceMap, collectedVariables);
    }

    @Test
    public void testReplaceVars_String_TaskVariableMap() {
        DisplayedTaskVariable var1 = createVar("var1");

        String replaceStr = "testReplaceVars_String_TaskVariableMap";
        TaskVariableMap varMap = variableMap(Collections.singletonMap(var1, replaceStr));
        String resultStr = replaceVars(var1.getScriptReplaceConstant(), varMap);
        assertEquals(replaceStr, resultStr);
    }

    @Test
    public void testReplaceVars_3args() {
        DisplayedTaskVariable var1 = createVar("var1");
        DisplayedTaskVariable var2 = createVar("var2");
        DisplayedTaskVariable var3 = createVar("var3", "display");
        DisplayedTaskVariable var4 = createVar("var4");

        String str = var1.getScriptReplaceConstant()
                + " SEPARATOR1 "
                + "${unknown-var}${illegal-chars*-=\\}}}}}"
                + var2.getScriptReplaceConstant()
                + var3.getScriptReplaceConstant()
                + " SEPARATOR2 "
                + var4.getScriptReplaceConstant();

        Map<DisplayedTaskVariable, String> valueMap = new HashMap<>();
        valueMap.put(var1, "VALUE1");
        valueMap.put(var2, "VALUE2");
        valueMap.put(var3, "VALUE3");
        valueMap.put(var4, "VALUE4");

        List<DisplayedTaskVariable> foundVars = new LinkedList<>();
        String resultStr = replaceVars(str, variableMap(valueMap), foundVars);
        assertEquals("VALUE1 SEPARATOR1 ${unknown-var}${illegal-chars*-=\\}}}}}VALUE2VALUE3 SEPARATOR2 VALUE4",
                resultStr);
    }
}
