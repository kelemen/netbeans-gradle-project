package org.netbeans.gradle.project.tasks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;

public final class GradleTaskDef {
    public static final class Builder {
        private final List<String> taskNames;
        private List<String> arguments;
        private List<String> jvmArguments;

        private TaskOutputListener stdOutListener;
        private TaskOutputListener stdErrListener;
        private boolean nonBlocking;

        public Builder(GradleTaskDef taskDef) {
            this.taskNames = taskDef.getTaskNames();
            this.arguments = taskDef.getArguments();
            this.jvmArguments = taskDef.getJvmArguments();
            this.stdOutListener = taskDef.getStdOutListener();
            this.stdErrListener = taskDef.getStdErrListener();
            this.nonBlocking = false;
        }

        public Builder(String taskName) {
            this(Collections.singletonList(taskName));
        }

        public Builder(String[] taskNames) {
            this(Arrays.asList(taskNames));
        }

        public Builder(List<String> taskNames) {
            this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
            this.arguments = Collections.emptyList();
            this.jvmArguments = Collections.emptyList();
            this.stdOutListener = NoOpTaskOutputListener.INSTANCE;
            this.stdErrListener = NoOpTaskOutputListener.INSTANCE;

            if (this.taskNames.isEmpty()) {
                throw new IllegalArgumentException("At least one task is required.");
            }
        }

        public boolean isNonBlocking() {
            return nonBlocking;
        }

        public void setNonBlocking(boolean nonBlocking) {
            this.nonBlocking = nonBlocking;
        }

        public List<String> getTaskNames() {
            return taskNames;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = CollectionUtils.copyNullSafeList(arguments);
        }

        public List<String> getJvmArguments() {
            return jvmArguments;
        }

        public void setJvmArguments(List<String> jvmArguments) {
            this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
        }

        public TaskOutputListener getStdOutListener() {
            return stdOutListener;
        }

        public void setStdOutListener(TaskOutputListener stdOutListener) {
            if (stdOutListener == null) throw new NullPointerException("stdOutListener");
            this.stdOutListener = stdOutListener;
        }

        public TaskOutputListener getStdErrListener() {
            return stdErrListener;
        }

        public void setStdErrListener(TaskOutputListener stdErrListener) {
            if (stdErrListener == null) throw new NullPointerException("stdErrListener");
            this.stdErrListener = stdErrListener;
        }

        public GradleTaskDef create() {
            return new GradleTaskDef(this);
        }
    }

    private final List<String> taskNames;
    private final List<String> arguments;
    private final List<String> jvmArguments;
    private final TaskOutputListener stdOutListener;
    private final TaskOutputListener stdErrListener;
    private final boolean nonBlocking;

    private GradleTaskDef(Builder builder) {
        this.taskNames = builder.getTaskNames();
        this.arguments = builder.getArguments();
        this.jvmArguments = builder.getJvmArguments();
        this.stdOutListener = builder.getStdOutListener();
        this.stdErrListener = builder.getStdErrListener();
        this.nonBlocking = builder.isNonBlocking();
    }

    private static String[] stringListToArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public boolean isNonBlocking() {
        return nonBlocking;
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

    public TaskOutputListener getStdOutListener() {
        return stdOutListener;
    }

    public TaskOutputListener getStdErrListener() {
        return stdErrListener;
    }

    private enum NoOpTaskOutputListener implements TaskOutputListener {
        INSTANCE;

        @Override
        public void receiveOutput(char[] buffer, int offset, int length) {
        }
    }
}
