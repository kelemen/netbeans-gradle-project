package org.netbeans.gradle.project.properties;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbGradleTask;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.tasks.GradleTaskDef;

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
            if (!projectName.isEmpty() && !projectName.startsWith(":")) {
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

    public static GradleTaskDef.Builder getDefaultTaskBuilder(
            NbGradleProject project,
            List<String> taskNames,
            boolean nonBlocking) {
        return getDefaultTaskBuilder(project.getDisplayName(), taskNames, nonBlocking);
    }

    public static GradleTaskDef.Builder getDefaultTaskBuilder(
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

    public GradleTaskDef createTaskDef(NbGradleProject project, NbGradleModule mainModule) {
        String projectName = mainModule.getUniqueName();
        List<String> processedTaskNames = new LinkedList<String>();
        for (Name name: taskNames) {
            String rawName = name.getName().replace("${project}", projectName);
            if (name.mustExist && !isTaskExists(mainModule, rawName)) {
                return null;
            }
            processedTaskNames.add(rawName);
        }

        GradleTaskDef.Builder builder = getDefaultTaskBuilder(
                project, processedTaskNames, nonBlocking);
        builder.setArguments(arguments);
        builder.setJvmArguments(jvmArguments);
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
