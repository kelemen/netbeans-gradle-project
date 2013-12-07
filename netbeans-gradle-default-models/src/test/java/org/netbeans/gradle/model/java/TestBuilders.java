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

    public static ProjectInfoBuilder<String> testProjectInfoBuilder(String prefix) {
        return new TestProjectInfoBuilder(prefix);
    }

    public static BuildInfoBuilder<Void> failingBuildInfoBuilder(String exceptionMessage) {
        return new FailingBuildInfoBuilder(exceptionMessage);
    }

    public static ProjectInfoBuilder<Void> failingProjectInfoBuilder(String exceptionMessage) {
        return new FailingProjectInfoBuilder(exceptionMessage);
    }

    public static BuildInfoBuilder<Void> notSerializableBuildInfoBuilder() {
        return new NotSerializableBuildInfoBuilder();
    }

    public static ProjectInfoBuilder<Void> notSerializableProjectInfoBuilder() {
        return new NotSerializableProjectInfoBuilder();
    }

    public static BuildInfoBuilder<Object> notSerializableResultBuildInfoBuilder() {
        return new NotSerializableResultBuildInfoBuilder();
    }

    public static ProjectInfoBuilder<Object> notSerializableResultProjectInfoBuilder() {
        return new NotSerializableResultProjectInfoBuilder();
    }

    private static final class NotSerializableResultBuildInfoBuilder implements BuildInfoBuilder<Object> {
        private static final long serialVersionUID = 1L;

        public final Object blockerOfSerialization = new Object();

        public Object getInfo(BuildController controller) {
            return new Object();
        }

        public String getName() {
            return getClass().getName();
        }
    }

    private static final class NotSerializableResultProjectInfoBuilder implements ProjectInfoBuilder<Object> {
        private static final long serialVersionUID = 1L;

        public Object getProjectInfo(Project project) {
            return new Object();
        }

        public String getName() {
            return getClass().getName();
        }
    }

    @SuppressWarnings("serial")
    private static final class NotSerializableProjectInfoBuilder implements ProjectInfoBuilder<Void> {
        public final Object blockerOfSerialization = new Object();

        public Void getProjectInfo(Project controller) {
            return null;
        }

        public String getName() {
            return getClass().getName();
        }
    }

    @SuppressWarnings("serial")
    private static final class NotSerializableBuildInfoBuilder implements BuildInfoBuilder<Void> {
        public final Object blockerOfSerialization = new Object();

        public Void getInfo(BuildController controller) {
            return null;
        }

        public String getName() {
            return getClass().getName();
        }
    }

    private static final class TestProjectInfoBuilder implements ProjectInfoBuilder<String> {
        private static final long serialVersionUID = 1L;

        private final String prefix;

        public TestProjectInfoBuilder(String prefix) {
            if (prefix == null) throw new NullPointerException("prefix");
            this.prefix = prefix;
        }

        public String getProjectInfo(Project project) {
            String rootName = project.getName();
            return prefix + rootName;
        }

        public String getName() {
            return BuilderUtils.getNameForGenericBuilder(this, prefix);
        }
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
