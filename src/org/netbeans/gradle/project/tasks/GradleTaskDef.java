package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.output.SmartOutputHandler;
import org.openide.util.Lookup;

public final class GradleTaskDef {
    public static final class Builder {
        private String caption;
        private final List<String> taskNames;
        private List<String> arguments;
        private List<String> jvmArguments;

        private SmartOutputHandler.Visitor stdOutListener;
        private SmartOutputHandler.Visitor stdErrListener;
        private boolean cleanOutput;
        private boolean reuseOutput;
        private boolean nonBlocking;

        public Builder(GradleTaskDef taskDef) {
            this.caption = taskDef.getCaption();
            this.taskNames = taskDef.getTaskNames();
            this.arguments = taskDef.getArguments();
            this.jvmArguments = taskDef.getJvmArguments();
            this.stdOutListener = taskDef.getStdOutListener();
            this.stdErrListener = taskDef.getStdErrListener();
            this.nonBlocking = taskDef.isNonBlocking();
            this.reuseOutput = taskDef.isReuseOutput();
            this.cleanOutput = taskDef.isCleanOutput();
        }

        public Builder(String caption, String taskName) {
            this(caption, Collections.singletonList(taskName));
        }

        public Builder(String caption, String[] taskNames) {
            this(caption, Arrays.asList(taskNames));
        }

        public Builder(String caption, List<String> taskNames) {
            if (caption == null) throw new NullPointerException("caption");

            this.caption = caption;
            this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
            this.arguments = Collections.emptyList();
            this.jvmArguments = Collections.emptyList();
            this.stdOutListener = NoOpTaskOutputListener.INSTANCE;
            this.stdErrListener = NoOpTaskOutputListener.INSTANCE;
            this.nonBlocking = false;
            this.reuseOutput = true;
            this.cleanOutput = false;

            if (this.taskNames.isEmpty()) {
                throw new IllegalArgumentException("At least one task is required.");
            }
        }

        public boolean isCleanOutput() {
            return cleanOutput;
        }

        public void setCleanOutput(boolean cleanOutput) {
            this.cleanOutput = cleanOutput;
        }

        public String getCaption() {
            return caption;
        }

        public void setCaption(String caption) {
            if (caption == null) throw new NullPointerException("caption");
            this.caption = caption;
        }

        public void setDefaultCaption(NbGradleProject project) {
            setDefaultCaption(project.getDisplayName());
        }

        public void setDefaultCaption(String projectName) {
            String newCaption = projectName;
            if (!nonBlocking) {
                newCaption += " - " + taskNames.toString();
            }
            this.caption = newCaption;
        }

        public boolean isReuseOutput() {
            return reuseOutput;
        }

        public void setReuseOutput(boolean reuseOutput) {
            this.reuseOutput = reuseOutput;
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

        private static <T> List<T> concatNullSafeLists(
                List<? extends T> list1,
                List<? extends T> list2) {
            List<T> result = new ArrayList<T>(list1.size() + list2.size());
            result.addAll(list1);
            result.addAll(list2);
            for (T element: result) {
                if (element == null) throw new NullPointerException("element");
            }
            return result;
        }

        public void addArguments(List<String> toAdd) {
            this.arguments = Collections.unmodifiableList(concatNullSafeLists(this.arguments, toAdd));
        }

        public List<String> getJvmArguments() {
            return jvmArguments;
        }

        public void setJvmArguments(List<String> jvmArguments) {
            this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
        }

        public void addJvmArguments(List<String> toAdd) {
            this.jvmArguments = Collections.unmodifiableList(concatNullSafeLists(this.jvmArguments, toAdd));
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

    private final String caption;
    private final List<String> taskNames;
    private final List<String> arguments;
    private final List<String> jvmArguments;
    private final SmartOutputHandler.Visitor stdOutListener;
    private final SmartOutputHandler.Visitor stdErrListener;
    private final boolean reuseOutput;
    private final boolean nonBlocking;
    private final boolean cleanOutput;

    private GradleTaskDef(Builder builder) {
        this.caption = builder.getCaption();
        this.taskNames = builder.getTaskNames();
        this.arguments = builder.getArguments();
        this.jvmArguments = builder.getJvmArguments();
        this.stdOutListener = builder.getStdOutListener();
        this.stdErrListener = builder.getStdErrListener();
        this.nonBlocking = builder.isNonBlocking();
        this.reuseOutput = builder.isReuseOutput();
        this.cleanOutput = builder.isCleanOutput();
    }

    private static String[] stringListToArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public boolean isCleanOutput() {
        return cleanOutput;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isReuseOutput() {
        return reuseOutput;
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

    private static List<String> processList(List<String> strings, TaskVariableMap varReplaceMap) {
        List<String> result = new ArrayList<String>(strings.size());
        for (String str: strings) {
            result.add(StandardTaskVariable.replaceVars(str, varReplaceMap));
        }
        return result;
    }

    public static GradleTaskDef.Builder createFromTemplate(
            String caption,
            GradleCommandTemplate command,
            TaskVariableMap varReplaceMap) {
        if (caption == null) throw new NullPointerException("caption");
        if (command == null) throw new NullPointerException("command");
        if (varReplaceMap == null) throw new NullPointerException("varReplaceMap");

        GradleTaskDef.Builder builder = new Builder(caption, processList(command.getTasks(), varReplaceMap));
        builder.setArguments(processList(command.getArguments(), varReplaceMap));
        builder.setJvmArguments(processList(command.getJvmArguments(), varReplaceMap));
        builder.setNonBlocking(!command.isBlocking());
        builder.setCleanOutput(command.isBlocking());
        builder.setReuseOutput(!command.isBlocking());
        return builder;
    }

    public static GradleTaskDef.Builder createFromTemplate(
            NbGradleProject project,
            GradleCommandTemplate command,
            TaskVariableMap varReplaceMap) {
        GradleTaskDef.Builder builder = createFromTemplate("", command, varReplaceMap);
        builder.setDefaultCaption(project);
        return builder;
    }

    public static GradleTaskDef.Builder createFromTemplate(
            String caption,
            GradleCommandTemplate command,
            NbGradleProject project,
            Lookup actionContext) {

        TaskVariableMap varReplaceMap = project.getVarReplaceMap(actionContext);
        return createFromTemplate(caption, command, varReplaceMap);
    }

    public static GradleTaskDef.Builder createFromTemplate(
            NbGradleProject project,
            GradleCommandTemplate command,
            Lookup actionContext) {
        TaskVariableMap varReplaceMap = project.getVarReplaceMap(actionContext);
        return createFromTemplate(project, command, varReplaceMap);
    }

    private enum NoOpTaskOutputListener implements SmartOutputHandler.Visitor {
        INSTANCE;

        @Override
        public void visitLine(String line) {
        }
    }
}
