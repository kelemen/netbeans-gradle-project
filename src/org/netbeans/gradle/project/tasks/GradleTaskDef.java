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

        private SmartOutputHandler.Visitor stdOutListener;
        private SmartOutputHandler.Visitor stdErrListener;
        private boolean nonBlocking;

        public Builder(GradleTaskDef taskDef) {
            this.taskNames = taskDef.getTaskNames();
            this.arguments = taskDef.getArguments();
            this.jvmArguments = taskDef.getJvmArguments();
            this.stdOutListener = taskDef.getStdOutListener();
            this.stdErrListener = taskDef.getStdErrListener();
            this.nonBlocking = taskDef.isNonBlocking();
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
            this.nonBlocking = false;

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

        public SmartOutputHandler.Visitor getStdOutListener() {
            return stdOutListener;
        }

        public void setStdOutListener(SmartOutputHandler.Visitor stdOutListener) {
            if (stdOutListener == null) throw new NullPointerException("stdOutListener");
            this.stdOutListener = stdOutListener;
        }

        public SmartOutputHandler.Visitor getStdErrListener() {
            return stdErrListener;
        }

        public void setStdErrListener(SmartOutputHandler.Visitor stdErrListener) {
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
    private final SmartOutputHandler.Visitor stdOutListener;
    private final SmartOutputHandler.Visitor stdErrListener;
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

    public SmartOutputHandler.Visitor getStdOutListener() {
        return stdOutListener;
    }

    public SmartOutputHandler.Visitor getStdErrListener() {
        return stdErrListener;
    }

    private enum NoOpTaskOutputListener implements SmartOutputHandler.Visitor {
        INSTANCE;

        @Override
        public void visitLine(String line) {
        }
    }
}
