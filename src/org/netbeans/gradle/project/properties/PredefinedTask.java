package org.netbeans.gradle.project.properties;

import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;

public final class PredefinedTask {
    public static final class Name {
        private final String name;
        private final boolean mustExist;

        public Name(String name, boolean mustExist) {
            if (name == null) throw new NullPointerException("name");
            this.name = name;
            this.mustExist = mustExist;
        }

        public String getName() {
            return name;
        }

        public boolean isMustExist() {
            return mustExist;
        }

        @Override
        public String toString() {
            return name + "[must exist=" + (mustExist ? "yes" : "no") + "]";
        }
    }

    private final String displayName;
    private final List<Name> taskNames;
    private final List<String> arguments;
    private final List<String> jvmArguments;

    public PredefinedTask(
            String displayName,
            List<Name> taskNames,
            List<String> arguments,
            List<String> jvmArguments) {
        if (displayName == null) throw new NullPointerException("displayName");

        this.displayName = displayName;
        this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
        this.arguments = CollectionUtils.copyNullSafeList(arguments);
        this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Name> getTaskNames() {
        return taskNames;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    @Override
    public String toString() {
        return "PredefinedTask{"
                + "displayName=" + displayName
                + ", taskNames=" + taskNames
                + ", arguments=" + arguments
                + ", jvmArguments=" + jvmArguments + '}';
    }
}
