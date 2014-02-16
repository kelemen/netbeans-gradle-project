package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class JavaTestModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Collection<JavaTestTask> testTasks;

    public JavaTestModel(Collection<JavaTestTask> testTasks) {
        this.testTasks = CollectionUtils.copyNullSafeList(testTasks);
    }

    public Collection<JavaTestTask> getTestTasks() {
        return testTasks;
    }

    public static JavaTestModel getDefaulTestModel(File projectDir) {
        JavaTestTask testTask = new JavaTestTask(
                "test",
                BasicFileUtils.getSubPath(projectDir, "build", "test-results"));

        return new JavaTestModel(Collections.singleton(testTask));
    }
}
