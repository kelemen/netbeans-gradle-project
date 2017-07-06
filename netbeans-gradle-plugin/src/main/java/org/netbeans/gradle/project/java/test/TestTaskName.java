package org.netbeans.gradle.project.java.test;

import java.util.Objects;
import org.openide.util.Lookup;

public final class TestTaskName {
    public static final String DEFAULT_TEST_TASK_NAME = "test";
    public static final String DEFAULT_CLEAN_TEST_TASK_NAME = "cleanTest";

    private final String taskName;

    public TestTaskName(String taskName) {
        this.taskName = Objects.requireNonNull(taskName, "taskName");
    }

    public String getTaskName() {
        return taskName;
    }

    public static String getTaskName(Lookup lookup) {
        String result = tryGetTaskName(lookup);
        return result != null ? result : DEFAULT_TEST_TASK_NAME;
    }

    public static String tryGetTaskName(Lookup lookup) {
        TestTaskName name = lookup.lookup(TestTaskName.class);
        return name != null ? name.taskName : null;
    }
}
