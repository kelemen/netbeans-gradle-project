package org.netbeans.gradle.model.java;

import org.gradle.tooling.BuildController;
import org.netbeans.gradle.model.BuildInfoBuilder;

// This BuildInfoBuilder can no longer be used for some reason.
// Gradle's Tooling API will not be able serialize it.
public final class TestBuildInfoBuilder implements BuildInfoBuilder<String> {
    private static final long serialVersionUID = 1L;

    private final String prefix;

    public TestBuildInfoBuilder(String prefix) {
        if (prefix == null) throw new NullPointerException("prefix");
        this.prefix = prefix;
    }

    public String getInfo(BuildController controller) {
        String rootName = controller.getBuildModel().getRootProject().getName();
        return prefix + rootName;
    }
}
