package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.gradle.tooling.BuildController;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.BuilderUtils;

public final class TestBuilders {
    public static BuildInfoBuilder<String> testBuildInfoBuilder(String prefix) {
        return new TestBuildInfoBuilder(prefix);
    }

    public static BuildInfoBuilder<Void> failingBuildInfoBuilder(String exceptionMessage) {
        return new FailingBuildInfoBuilder(exceptionMessage);
    }

    public static ProjectInfoBuilder<Void> failingProjectInfoBuilder(String exceptionMessage) {
        return new FailingProjectInfoBuilder(exceptionMessage);
    }

    private static final class TestBuildInfoBuilder implements BuildInfoBuilder<String> {
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

    private static final class FailingBuildInfoBuilder implements BuildInfoBuilder<Void> {
        private static final long serialVersionUID = 1L;

        private final String exceptionMessage;

        public FailingBuildInfoBuilder(String exceptionMessage) {
            if (exceptionMessage == null) throw new NullPointerException("exceptionMessage");
            this.exceptionMessage = exceptionMessage;
        }

        public Void getInfo(BuildController controller) {
            throw new NotSerializableException(exceptionMessage);
        }

        public String getName() {
            return BuilderUtils.getNameForGenericBuilder(this, exceptionMessage);
        }
    }

    private static final class FailingProjectInfoBuilder implements ProjectInfoBuilder<Void> {
        private static final long serialVersionUID = 1L;

        private final String exceptionMessage;

        public FailingProjectInfoBuilder(String exceptionMessage) {
            if (exceptionMessage == null) throw new NullPointerException("exceptionMessage");
            this.exceptionMessage = exceptionMessage;
        }

        public Void getProjectInfo(Project project) {
            throw new NotSerializableException(exceptionMessage);
        }

        public String getName() {
            return BuilderUtils.getNameForGenericBuilder(this, exceptionMessage);
        }
    }

    @SuppressWarnings("serial")
    private static final class NotSerializableException extends RuntimeException {
        public final Object blockerOfSerialization;

        public NotSerializableException(String message) {
            super(message);
            blockerOfSerialization = new Object();
        }
    }

    private TestBuilders() {
        throw new AssertionError();
    }
}
