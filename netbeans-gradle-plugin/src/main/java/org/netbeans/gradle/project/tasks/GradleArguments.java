package org.netbeans.gradle.project.tasks;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.task.DaemonTaskContext;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

public final class GradleArguments {
    private static <V> List<V> emptyIfNull(List<V> list) {
        return list != null ? list : Collections.<V>emptyList();
    }

    public static List<String> getExtraArgs(
            Project project,
            Path preferredSettings,
            DaemonTaskContext context) {

        List<String> result = new LinkedList<>();

        result.addAll(emptyIfNull(GlobalGradleSettings.getDefault().gradleArgs().getValue()));

        if (preferredSettings != null) {
            result.add("-c");
            result.add(preferredSettings.toString());
        }

        if (context.isModelLoading()) {
            result.add("-PevaluatingIDE=NetBeans");
        }

        return result;
    }

    public static List<String> getExtraJvmArgs(
            Project project,
            DaemonTaskContext context) {
        return emptyIfNull(GlobalGradleSettings.getDefault().gradleJvmArgs().getValue());
    }

    private GradleArguments() {
        throw new AssertionError();
    }
}
