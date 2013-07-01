package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.gradle.tooling.internal.gradle.DefaultGradleProject;
import org.gradle.tooling.internal.gradle.DefaultGradleTask;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class PredefinedTaskTest {
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

    private static List<PredefinedTask.Name> createTaskNames(boolean mustExist, String... names) {
        List<PredefinedTask.Name> result = new ArrayList<PredefinedTask.Name>(names.length);
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

    private static List<GradleTask> gradleTasks(GradleProject project, String... names) {
        List<GradleTask> result = new ArrayList<GradleTask>(names.length);
        for (String name: names) {
            DefaultGradleTask task = new DefaultGradleTask();
            task.setName(name);
            task.setProject(project);
            result.add(task);
        }
        return result;
    }

    private static GradleProject createDummyProject() {
        DefaultGradleProject root = new DefaultGradleProject(":");
        root.setTasks(gradleTasks(root, "rootTask1", "rootTask2"));

        DefaultGradleProject sub1 = new DefaultGradleProject(":sub1");
        sub1.setTasks(gradleTasks(sub1, "sub1Task1", "sub1Task2"));

        DefaultGradleProject sub2 = new DefaultGradleProject(":sub2");

        sub1.setParent(root);
        sub2.setParent(root);
        root.setChildren(Arrays.asList(sub1, sub2));

        DefaultGradleProject sub2_1 = new DefaultGradleProject(":sub2:subsub1");
        DefaultGradleProject sub2_2 = new DefaultGradleProject(":sub2:subsub2");
        sub2_2.setTasks(gradleTasks(sub2_2, "sub2_2Task1", "sub2_2Task2"));

        sub2_1.setParent(sub2);
        sub2_2.setParent(sub2);
        sub2.setChildren(Arrays.asList(sub2_1, sub2_2));

        return root;
    }

    private static PredefinedTask createTestTask(String name, boolean mustExist) {
        return new PredefinedTask("DISPLAY-NAME",
                createTaskNames(mustExist, name),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true);
    }

    private void taskExistsInProject(
            GradleProject project,
            String taskName,
            TaskVariableMap varMap) {
        assertTrue(taskName + " should be found in the project",
                createTestTask(taskName, true).isTasksExistsIfRequired(project, varMap));
    }

    private void taskExistsInProject(GradleProject project, String taskName) {
        taskExistsInProject(project, taskName, EmptyVarMap.INSTANCE);
    }

    private void taskDoesNotExistInProject(
            GradleProject project,
            String taskName,
            TaskVariableMap varMap) {
        assertFalse(taskName + " should not be found in the project",
                createTestTask(taskName, true).isTasksExistsIfRequired(project, varMap));
    }

    private void taskDoesNotExistInProject(GradleProject project, String taskName) {
        taskDoesNotExistInProject(project, taskName, EmptyVarMap.INSTANCE);
    }

    @Test
    public void testIsTasksExistsIfRequired_MustExist_DoesExist() {
        GradleProject project = createDummyProject();

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
        GradleProject project = createDummyProject();

        taskDoesNotExistInProject(project, ":sub1:rootTask1");
        taskDoesNotExistInProject(project, ":sub1:rootTask1");
        taskDoesNotExistInProject(project, "");
        taskDoesNotExistInProject(project, ":");
        taskDoesNotExistInProject(project, ":unknown");
        taskDoesNotExistInProject(project, ":unknown:");
    }

    @Test
    public void testIsTasksExistsIfRequired_MustExist_WithVar() {
        GradleProject project = createDummyProject();

        taskExistsInProject(project, ":root${var1}", singletonVarMap("var1", "Task1"));
        taskExistsInProject(project, "${testVar}:sub1Task1", singletonVarMap("testVar", ":sub1"));
        taskExistsInProject(project, "${empty}:sub2:subsub2:sub2_2Task1", singletonVarMap("empty", ""));
    }

    private static TaskVariableMap singletonVarMap(final String name, final String value) {
        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                if (variable.getVariableName().equals(name)) {
                    return value;
                }
                else {
                    return null;
                }
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
