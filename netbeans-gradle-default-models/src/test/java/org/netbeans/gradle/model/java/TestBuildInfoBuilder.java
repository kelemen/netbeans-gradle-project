package org.netbeans.gradle.model.java;

import org.gradle.tooling.BuildController;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.util.BuilderUtils;

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

    public String getName() {
        return BuilderUtils.getNameForGenericBuilder(this, prefix);
    }
}
