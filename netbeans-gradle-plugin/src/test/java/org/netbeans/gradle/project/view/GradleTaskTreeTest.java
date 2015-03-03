package org.netbeans.gradle.project.view;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.netbeans.gradle.model.GradleTaskID;

import static org.junit.Assert.*;

public class GradleTaskTreeTest {
    private static List<GradleTaskID> toTaskIDs(String... taskNames) {
        List<GradleTaskID> result = new ArrayList<>(taskNames.length);
        for (String taskName: taskNames) {
            result.add(new GradleTaskID(taskName, ":Project:" + taskName));
        }
        return result;
    }

    private static void assertLeaf(
            GradleTaskTree tree,
            String expectedCaption) {

        assertEquals("caption", expectedCaption, tree.getCaption());
        assertEquals("Children count", 0, tree.getChildren().size());

        GradleTaskID leaf = tree.getTaskID();
        assertNotNull("leaf", leaf);
        assertEquals("task name", expectedCaption, leaf.getName());
    }

    private static void assertTasks(
            GradleTaskTree tree,
            String expectedCaption,
            String... expectedChildren) {

        assertEquals("caption", expectedCaption, tree.getCaption());
        assertNull("leaf", tree.getTaskID());

        List<GradleTaskTree> children = tree.getChildren();
        assertEquals("Children count", expectedChildren.length, children.size());

        int index = 0;
        for (GradleTaskTree child: children) {
            assertLeaf(child, expectedChildren[index]);
            index++;
        }
    }

    @Test(timeout = 10000)
    public void testNoInfiniteLoop1() {
        List<GradleTaskID> taskIDs = toTaskIDs("A");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(0, taskIDs);
        assertEquals("Node count", 1, nodes.size());

        assertLeaf(nodes.get(0), "A");
    }

    @Test(timeout = 10000)
    public void testNoInfiniteLoop2() {
        List<GradleTaskID> taskIDs = toTaskIDs("A", "B");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(0, taskIDs);
        assertEquals("Node count", 2, nodes.size());

        assertLeaf(nodes.get(0), "A");
        assertLeaf(nodes.get(1), "B");
    }

    @Test
    public void testSingleTask() {
        List<GradleTaskID> taskIDs = toTaskIDs("TASK");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(5, taskIDs);
        assertEquals("Node count", 1, nodes.size());

        assertLeaf(nodes.get(0), "TASK");
    }

    @Test
    public void testUnderscoreSeparated() {
        List<GradleTaskID> taskIDs = toTaskIDs(
                "TASK_A",
                "TASK_B",
                "TASK_C",
                "TASK_D",
                "TASK_E",
                "OTHER_A",
                "OTHER_B",
                "OTHER_C");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(5, taskIDs);
        assertEquals("Node count", 2, nodes.size());

        assertTasks(nodes.get(0), "TASK",
                "TASK_A",
                "TASK_B",
                "TASK_C",
                "TASK_D",
                "TASK_E");
        assertTasks(nodes.get(1), "OTHER",
                "OTHER_A",
                "OTHER_B",
                "OTHER_C");
    }

    @Test
    public void testBasicSingleLevelSplit() {
        List<GradleTaskID> taskIDs = toTaskIDs(
                "",
                "compileGroovy",
                "compileJava",
                "compileTestJava",
                "compileTestGroovy",
                "build",
                "buildSubA",
                "buildSubB");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(7, taskIDs);
        assertEquals("Node count", 3, nodes.size());

        assertLeaf(nodes.get(0), "");
        assertTasks(nodes.get(1), "compile",
                "compileGroovy",
                "compileJava",
                "compileTestJava",
                "compileTestGroovy");
        assertTasks(nodes.get(2), "build",
                "build",
                "buildSubA",
                "buildSubB");
    }

    @Test
    public void testMultiLevelUnderscoreSeparated() {
        List<GradleTaskID> taskIDs = toTaskIDs(
                "TASK__SUBA__A",
                "TASK__SUBA__B",
                "TASK__SUBB__A",
                "TASK__SUBB__B",
                "TASK__SUBB__C",
                "OTHER");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(3, taskIDs);
        assertEquals("Node count", 2, nodes.size());

        assertLeaf(nodes.get(1), "OTHER");

        GradleTaskTree subNode = nodes.get(0);
        assertEquals("caption", "TASK", subNode.getCaption());

        List<GradleTaskTree> subNodeChildren = subNode.getChildren();
        assertEquals("Node count", 2, subNodeChildren.size());

        assertTasks(subNodeChildren.get(0), "TASK__SUBA",
                "TASK__SUBA__A",
                "TASK__SUBA__B");
        assertTasks(subNodeChildren.get(1), "TASK__SUBB",
                "TASK__SUBB__A",
                "TASK__SUBB__B",
                "TASK__SUBB__C");
    }

    @Test
    public void testSingleLevelNoUnnecessaryIndirection() {
        List<GradleTaskID> taskIDs = toTaskIDs(
                "buildApp1",
                "buildApp2",
                "buildLib1",
                "buildLib2",
                "buildLib3");

        List<GradleTaskTree> nodes = GradleTaskTree.createTaskTree(4, taskIDs);
        assertEquals("Node count", 2, nodes.size());

        assertTasks(nodes.get(0), "buildApp",
                "buildApp1",
                "buildApp2");
        assertTasks(nodes.get(1), "buildLib",
                "buildLib1",
                "buildLib2",
                "buildLib3");
    }
}
