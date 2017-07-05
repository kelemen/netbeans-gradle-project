package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.NbGradleMultiProjectDef;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.netbeans.gradle.project.tasks.vars.VariableResolvers;

import static org.junit.Assert.*;

public class PredefinedTaskTest {
    private static List<PredefinedTask.Name> createTaskNames(boolean mustExist, String... names) {
        List<PredefinedTask.Name> result = new ArrayList<>(names.length);
        for (String name: names) {
            result.add(new PredefinedTask.Name(name, mustExist));
        }
        return result;
    }

    private void assertEqualNames(List<PredefinedTask.Name> expected, List<PredefinedTask.Name> actual) {
        PredefinedTask.Name[] expectedArray = expected.toArray(new PredefinedTask.Name[0]);
        PredefinedTask.Name[] actualArray = actual.toArray(new PredefinedTask.Name[0]);

        if (expectedArray.length != actualArray.length) {
            fail("The size of the arrays differ. Expected: " + expectedArray.length
                    + ". Actual: " + actualArray.length);
        }

        for (int i = 0; i < actualArray.length; i++) {
            PredefinedTask.Name expectedElement = expectedArray[i];
            PredefinedTask.Name actualElement = actualArray[i];

            assertEquals(expectedElement.getName(), actualElement.getName());
            assertEquals(expectedElement.isMustExist(), actualElement.isMustExist());
        }
    }

    @Test
    public void testProperties() {
        String displayName = "DISPLAY-NAME";
        List<PredefinedTask.Name> taskNames = createTaskNames(true, "task1", "", "task2");
        List<String> arguments = Arrays.asList("arg1", "", "arg2");
        List<String> jvmArguments = Arrays.asList("jvmarg1", "", "jvmarg2");
        boolean nonBlocking = false;

        PredefinedTask task = new PredefinedTask(displayName, taskNames, arguments, jvmArguments, nonBlocking);
        assertEquals(displayName, task.getDisplayName());
        assertEqualNames(taskNames, task.getTaskNames());
        assertEquals(arguments, task.getArguments());
        assertEquals(jvmArguments, task.getJvmArguments());
        assertEquals(nonBlocking, task.isNonBlocking());
        assertTrue(task.toString().contains(displayName));
    }

    /**
     * Test of createSimple method, of class PredefinedTask.
     */
    @Test
    public void testCreateSimple() {
        String displayName = "DISPLAY-NAME";
        String taskName = "TASK-NAME";

        PredefinedTask task = PredefinedTask.createSimple(displayName, taskName);

        assertEquals(displayName, task.getDisplayName());
        assertEqualNames(createTaskNames(false, taskName), task.getTaskNames());
        assertEquals(Collections.emptyList(), task.getArguments());
        assertEquals(Collections.emptyList(), task.getJvmArguments());
        assertFalse(task.isNonBlocking());
    }

    /**
     * Test of toCommandTemplate method, of class PredefinedTask.
     */
    @Test
    public void testToCommandTemplate() {
        String displayName = "DISPLAY-NAME";
        List<PredefinedTask.Name> taskNames = createTaskNames(true, "task1", "", "task2");
        List<String> arguments = Arrays.asList("arg1", "", "arg2");
        List<String> jvmArguments = Arrays.asList("jvmarg1", "", "jvmarg2");
        boolean nonBlocking = false;

        PredefinedTask task = new PredefinedTask(displayName, taskNames, arguments, jvmArguments, nonBlocking);
        GradleCommandTemplate commandTemplate = task.toCommandTemplate();

        assertEquals(Arrays.asList("task1", "", "task2"), commandTemplate.getTasks());
        assertEquals(arguments, commandTemplate.getArguments());
        assertEquals(jvmArguments, commandTemplate.getJvmArguments());
        assertTrue(commandTemplate.isBlocking());
    }

    private static List<GradleTaskID> gradleTasks(String projectFullName, String... names) {
        List<GradleTaskID> result = new ArrayList<>(names.length);
        for (String name: names) {
            String fullName = projectFullName.endsWith(":")
                    ? projectFullName + name
                    : projectFullName + ":" + name;
            result.add(new GradleTaskID(name, fullName));
        }
        return result;
    }

    private static NbGradleProjectTree createProject(
            String fullName,
            Collection<GradleTaskID> tasks,
            Collection<NbGradleProjectTree> subProjects) {
        String[] names = fullName.split(":");
        String name = names.length > 0 ? names[names.length - 1] : "";
        GenericProjectProperties properties = new GenericProjectProperties(
                name,
                fullName,
                new File(""),
                new File(CommonScripts.BUILD_BASE_NAME + CommonScripts.DEFAULT_SCRIPT_EXTENSION));

        return new NbGradleProjectTree(properties, tasks, subProjects);
    }

    private static NbGradleProjectTree sub2Sub2() {
        String fullName = ":sub2:subsub2";

        return createProject(fullName,
                gradleTasks(fullName, "sub2_2Task1", "sub2_2Task2"),
                Collections.<NbGradleProjectTree>emptyList());
    }

    private static NbGradleProjectTree sub2Sub1() {
        String fullName = ":sub2:subsub1";

        return createProject(fullName,
                Collections.<GradleTaskID>emptyList(),
                Collections.<NbGradleProjectTree>emptyList());
    }

    private static NbGradleProjectTree sub2() {
        String fullName = ":sub2";

        return createProject(fullName,
                Collections.<GradleTaskID>emptyList(),
                Arrays.asList(sub2Sub1(), sub2Sub2()));
    }

    private static NbGradleProjectTree sub1() {
        String fullName = ":sub1";

        return createProject(fullName,
                gradleTasks(fullName, "sub1Task1", "sub1Task2"),
                Collections.<NbGradleProjectTree>emptyList());
    }

    private static NbGradleProjectTree rootProject() {
        String fullName = ":";

        return createProject(fullName,
                gradleTasks(fullName, "rootTask1", "rootTask2"),
                Arrays.asList(sub1(), sub2()));
    }

    private static NbGradleMultiProjectDef createDummyProject() {
        NbGradleProjectTree root = rootProject();
        return new NbGradleMultiProjectDef(root, root);
    }

    private static PredefinedTask createTestTask(String name, boolean mustExist) {
        return new PredefinedTask("DISPLAY-NAME",
                createTaskNames(mustExist, name),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
    }

    private static boolean isTasksExistsIfRequired(
            PredefinedTask task,
            NbGradleMultiProjectDef project,
            TaskVariableMap varMap) {
        StringResolver resolver = StringResolvers.bindVariableResolver(VariableResolvers.getDefault(), varMap);
        return task.isTasksExistsIfRequired(project, resolver);
    }

    private void taskExistsInProject(
            NbGradleMultiProjectDef project,
            String taskName,
            TaskVariableMap varMap) {
        assertTrue(taskName + " should be found in the project",
                isTasksExistsIfRequired(createTestTask(taskName, true), project, varMap));
    }

    private void taskExistsInProject(NbGradleMultiProjectDef project, String taskName) {
        taskExistsInProject(project, taskName, EmptyVarMap.INSTANCE);
    }

    private void taskDoesNotExistInProject(
            NbGradleMultiProjectDef project,
            String taskName,
            TaskVariableMap varMap) {
        assertFalse(taskName + " should not be found in the project",
                isTasksExistsIfRequired(createTestTask(taskName, true), project, varMap));
    }

    private void taskDoesNotExistInProject(NbGradleMultiProjectDef project, String taskName) {
        taskDoesNotExistInProject(project, taskName, EmptyVarMap.INSTANCE);
    }

    @Test
    public void testIsTasksExistsIfRequired_MustExist_DoesExist() {
        NbGradleMultiProjectDef project = createDummyProject();

        taskExistsInProject(project, ":rootTask1");
        taskExistsInProject(project, ":rootTask2");

        taskExistsInProject(project, ":sub1:sub1Task1");
        taskExistsInProject(project, ":sub1:sub1Task2");

        taskExistsInProject(project, ":sub2:subsub2:sub2_2Task1");
        taskExistsInProject(project, ":sub2:subsub2:sub2_2Task2");

        taskExistsInProject(project, "rootTask1");
        taskExistsInProject(project, "rootTask2");
        taskExistsInProject(project, "sub1Task1");
        taskExistsInProject(project, "sub1Task2");
        taskExistsInProject(project, "sub2_2Task1");
        taskExistsInProject(project, "sub2_2Task2");
    }

    @Test
    public void testIsTasksExistsIfRequired_MustExist_DoesntExist() {
        NbGradleMultiProjectDef project = createDummyProject();

        taskDoesNotExistInProject(project, ":sub1:rootTask1");
        taskDoesNotExistInProject(project, ":sub1:rootTask1");
        taskDoesNotExistInProject(project, "");
        taskDoesNotExistInProject(project, ":");
        taskDoesNotExistInProject(project, ":unknown");
        taskDoesNotExistInProject(project, ":unknown:");
    }

    @Test
    public void testIsTasksExistsIfRequired_MustExist_WithVar() {
        NbGradleMultiProjectDef project = createDummyProject();

        taskExistsInProject(project, ":root${var1}", singletonVarMap("var1", "Task1"));
        taskExistsInProject(project, "${testVar}:sub1Task1", singletonVarMap("testVar", ":sub1"));
        taskExistsInProject(project, "${empty}:sub2:subsub2:sub2_2Task1", singletonVarMap("empty", ""));
    }

    private static TaskVariableMap singletonVarMap(final String name, final String value) {
        return (TaskVariable variable) -> {
            if (variable.getVariableName().equals(name)) {
                return value;
            }
            else {
                return null;
            }
        };
    }

    private enum EmptyVarMap implements TaskVariableMap {
        INSTANCE;

        @Override
        public String tryGetValueForVariable(TaskVariable variable) {
            return null;
        }
    }
}
