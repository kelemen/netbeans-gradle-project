package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GradleTaskDef {
    public static final class Builder {
        private final List<String> taskNames;
        private List<String> arguments;
        private List<String> jvmArguments;

        private static List<String> wrapStringList(List<String> stringList) {
            if (stringList == null) throw new NullPointerException("stringList");

            List<String> wrapped = Collections.unmodifiableList(new ArrayList<String>(stringList));
            for (String element: wrapped) {
                if (element == null) throw new NullPointerException("element");
            }
            return wrapped;
        }

        public Builder(GradleTaskDef taskDef) {
            this.taskNames = taskDef.getTaskNames();
            this.arguments = taskDef.getArguments();
            this.jvmArguments = taskDef.getJvmArguments();
        }

        public Builder(String taskName) {
            this(Collections.singletonList(taskName));
        }

        public Builder(String[] taskNames) {
            this(Arrays.asList(taskNames));
        }

        public Builder(List<String> taskNames) {
            this.taskNames = wrapStringList(taskNames);
            this.arguments = Collections.emptyList();
            this.jvmArguments = Collections.emptyList();

            if (this.taskNames.isEmpty()) {
                throw new IllegalArgumentException("At least one task is required.");
            }
        }

        public List<String> getTaskNames() {
            return taskNames;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = wrapStringList(arguments);
        }

        public List<String> getJvmArguments() {
            return jvmArguments;
        }

        public void setJvmArguments(List<String> jvmArguments) {
            this.jvmArguments = wrapStringList(jvmArguments);
        }

        public GradleTaskDef create() {
            return new GradleTaskDef(this);
        }
    }

    private final List<String> taskNames;
    private final List<String> arguments;
    private final List<String> jvmArguments;

    private GradleTaskDef(Builder builder) {
        this.taskNames = builder.getTaskNames();
        this.arguments = builder.getArguments();
        this.jvmArguments = builder.getJvmArguments();
    }

    private static String[] stringListToArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public List<String> getTaskNames() {
        return taskNames;
    }

    public String[] getTaskNamesArray() {
        return stringListToArray(taskNames);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String[] getArgumentArray() {
        return stringListToArray(arguments);
    }

    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    public String[] getJvmArgumentsArray() {
        return stringListToArray(jvmArguments);
    }
}
