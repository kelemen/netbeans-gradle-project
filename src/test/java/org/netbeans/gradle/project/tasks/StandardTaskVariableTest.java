package org.netbeans.gradle.project.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

import static org.junit.Assert.*;

public class StandardTaskVariableTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static TaskVariableMap variableMap(Map<DisplayedTaskVariable, String> map) {
        final Map<TaskVariable, String> appliedMap = new HashMap<TaskVariable, String>();
        for (Map.Entry<DisplayedTaskVariable, String> entry: map.entrySet()) {
            appliedMap.put(entry.getKey().getVariable(), entry.getValue());
        }

        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                return appliedMap.get(variable);
            }
        };
    }

    /**
     * Test of replaceVars method, of class StandardTaskVariable.
     */
    @Test
    public void testReplaceVars_String_TaskVariableMap() {
        DisplayedTaskVariable var1 = new DisplayedTaskVariable("var1");

        String replaceStr = "testReplaceVars_String_TaskVariableMap";
        TaskVariableMap varMap = variableMap(Collections.singletonMap(var1, replaceStr));
        String resultStr = StandardTaskVariable.replaceVars(var1.getScriptReplaceConstant(), varMap);
        assertEquals(replaceStr, resultStr);
    }

    /**
     * Test of replaceVars method, of class StandardTaskVariable.
     */
    @Test
    public void testReplaceVars_3args() {
        DisplayedTaskVariable var1 = new DisplayedTaskVariable("var1");
        DisplayedTaskVariable var2 = new DisplayedTaskVariable("var2");
        DisplayedTaskVariable var3 = new DisplayedTaskVariable("var3", "display");
        DisplayedTaskVariable var4 = new DisplayedTaskVariable("var4");

        String str = var1.getScriptReplaceConstant()
                + " SEPARATOR1 "
                + "${unknown-var}${illegal-chars*-=\\}}}}}"
                + var2.getScriptReplaceConstant()
                + var3.getScriptReplaceConstant()
                + " SEPARATOR2 "
                + var4.getScriptReplaceConstant();

        Map<DisplayedTaskVariable, String> valueMap = new HashMap<DisplayedTaskVariable, String>();
        valueMap.put(var1, "VALUE1");
        valueMap.put(var2, "VALUE2");
        valueMap.put(var3, "VALUE3");
        valueMap.put(var4, "VALUE4");

        List<DisplayedTaskVariable> foundVars = new LinkedList<DisplayedTaskVariable>();
        String resultStr = StandardTaskVariable.replaceVars(str, variableMap(valueMap), foundVars);
        assertEquals("VALUE1 SEPARATOR1 ${unknown-var}${illegal-chars*-=\\}}}}}VALUE2VALUE3 SEPARATOR2 VALUE4",
                resultStr);
    }
}
