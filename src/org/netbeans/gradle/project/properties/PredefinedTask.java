package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbGradleTask;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.openide.util.Lookup;

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
    private final boolean nonBlocking;

    public PredefinedTask(
            String displayName,
            List<Name> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            boolean nonBlocking) {
        if (displayName == null) throw new NullPointerException("displayName");

        this.displayName = displayName;
        this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
        this.arguments = CollectionUtils.copyNullSafeList(arguments);
        this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
        this.nonBlocking = nonBlocking;
    }

    public boolean isNonBlocking() {
        return nonBlocking;
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

    public static PredefinedTask createSimple(String displayName, String taskName) {
        Name name = new Name(taskName, false);
        return new PredefinedTask(displayName,
                Arrays.asList(name),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                false);
    }

    private static boolean isLocalTaskExists(NbGradleModule module, String task) {
        for (NbGradleTask moduleTask: module.getTasks()) {
            if (moduleTask.getLocalName().equals(task)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalTaskExistsInModuleTree(NbGradleModule module, String task) {
        if (isLocalTaskExists(module, task)) {
            return true;
        }
        for (NbGradleModule child: module.getChildren()) {
            if (isLocalTaskExistsInModuleTree(child, task)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTaskExists(NbGradleModule module, String task) {
        String projectName;
        String localName;

        int nameSepIndex = task.lastIndexOf(':');
        if (nameSepIndex >= 0) {
            projectName = task.substring(0, nameSepIndex);
            localName = task.substring(nameSepIndex + 1);

            // if projectName is empty, that means that we want to execute
            // the task of the root project.
            if (projectName.isEmpty()) {
                projectName = ":";
            }
            else if (!projectName.startsWith(":")) {
                projectName = module.getUniqueName() + ":" + projectName;
            }
        }
        else {
            projectName = null;
            localName = task;
        }

        if (projectName != null) {
            NbGradleModule projectModule = NbModelUtils.lookupModuleByName(module, projectName);
            if (projectModule == null) {
                return false;
            }

            return isLocalTaskExists(projectModule, localName);
        }
        else {
            if (isLocalTaskExistsInModuleTree(module, localName)) {
                return true;
            }

            return false;
        }
    }

    private static GradleTaskDef.Builder getDefaultTaskBuilder(
            NbGradleProject project,
            List<String> taskNames,
            boolean nonBlocking) {
        return getDefaultTaskBuilder(project.getDisplayName(), taskNames, nonBlocking);
    }

    private static GradleTaskDef.Builder getDefaultTaskBuilder(
            String projectName,
            List<String> taskNames,
            boolean nonBlocking) {

        String caption = projectName;
        if (!nonBlocking) {
            caption += " - " + taskNames.toString();
        }

        GradleTaskDef.Builder builder = new GradleTaskDef.Builder(caption, taskNames);
        builder.setNonBlocking(nonBlocking);
        builder.setCleanOutput(!nonBlocking);
        builder.setReuseOutput(nonBlocking);
        return builder;
    }

    private static List<String> processList(List<String> strings, TaskVariableMap varReplaceMap) {
        List<String> result = new ArrayList<String>(strings.size());
        for (String str: strings) {
            result.add(StandardTaskVariable.replaceVars(str, varReplaceMap));
        }
        return result;
    }

    public GradleTaskDef.Builder createTaskDefBuilder(String caption, TaskVariableMap varReplaceMap) {
        List<String> processedTaskNames = new LinkedList<String>();
        for (Name name: taskNames) {
            processedTaskNames.add(StandardTaskVariable.replaceVars(name.getName(), varReplaceMap));
        }

        GradleTaskDef.Builder builder = new GradleTaskDef.Builder(caption, processedTaskNames);
        builder.setArguments(processList(arguments, varReplaceMap));
        builder.setJvmArguments(processList(jvmArguments, varReplaceMap));

        builder.setNonBlocking(nonBlocking);
        builder.setCleanOutput(!nonBlocking);
        builder.setReuseOutput(nonBlocking);

        return builder;
    }

    public GradleTaskDef tryCreateTaskDef(NbGradleProject project, Lookup actionContext) {
        return tryCreateTaskDef(project, project.getVarReplaceMap(actionContext));
    }

    public GradleTaskDef tryCreateTaskDef(NbGradleProject project, TaskVariableMap varReplaceMap) {
        NbGradleModule mainModule = project.getAvailableModel().getMainModule();

        List<String> processedTaskNames = new LinkedList<String>();
        for (Name name: taskNames) {
            String processName = StandardTaskVariable.replaceVars(name.getName(), varReplaceMap);
            if (name.mustExist && !isTaskExists(mainModule, processName)) {
                return null;
            }
            processedTaskNames.add(processName);
        }

        GradleTaskDef.Builder builder = getDefaultTaskBuilder(
                project, processedTaskNames, nonBlocking);
        builder.setArguments(processList(arguments, varReplaceMap));
        builder.setJvmArguments(processList(jvmArguments, varReplaceMap));
        return builder.create();
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
