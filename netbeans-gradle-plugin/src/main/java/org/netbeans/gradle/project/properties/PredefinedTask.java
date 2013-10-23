package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.NbGradleMultiProjectDef;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
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

    private static NbGradleProjectTree findProject(NbGradleMultiProjectDef project, String projectPath) {
        if (projectPath.isEmpty()) {
            return project.getRootProject();
        }

        if (projectPath.startsWith(":")) {
            return project.getRootProject().findByPath(projectPath);
        }
        else {
            return project.getMainProject().findByPath(projectPath);
        }
    }

    private static boolean isProjectHasTask(NbGradleProjectTree project, String taskName) {
        for (GradleTaskID task: project.getTasks()) {
            if (taskName.equals(task.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProjectOrChildrenHasTask(NbGradleMultiProjectDef project, String taskName) {
        return isProjectOrChildrenHasTask(project.getMainProject(), taskName);
    }

    private static boolean isProjectOrChildrenHasTask(NbGradleProjectTree project, String taskName) {
        if (isProjectHasTask(project, taskName)) {
            return true;
        }
        for (NbGradleProjectTree child: project.getChildren()) {
            if (isProjectOrChildrenHasTask(child, taskName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTaskExists(NbGradleMultiProjectDef project, String projectPath, String taskName) {
        NbGradleProjectTree taskProject = findProject(project, projectPath);
        if (taskProject == null) {
            return false;
        }

        return isProjectHasTask(taskProject, taskName);
    }

    private static boolean isTaskExists(NbGradleMultiProjectDef project, String taskName) {
        int taskNameSepIndex = taskName.lastIndexOf(':');
        if (taskNameSepIndex >= 0) {
            return isTaskExists(project,
                    taskName.substring(0, taskNameSepIndex),
                    taskName.substring(taskNameSepIndex + 1));
        }
        else {
            return isProjectOrChildrenHasTask(project, taskName);
        }
    }

    public GradleCommandTemplate toCommandTemplate(String displayName) {
        List<String> rawTaskNames = new ArrayList<String>(taskNames.size());
        for (Name name: taskNames) {
            rawTaskNames.add(name.getName());
        }

        GradleCommandTemplate.Builder builder
                = new GradleCommandTemplate.Builder(displayName, rawTaskNames);

        builder.setArguments(arguments);
        builder.setJvmArguments(jvmArguments);
        builder.setBlocking(!nonBlocking);
        return builder.create();
    }

    public GradleCommandTemplate toCommandTemplate() {
        return toCommandTemplate(displayName);
    }

    public boolean isTasksExistsIfRequired(NbGradleProject project, Lookup actionContext) {
        return isTasksExistsIfRequired(project, project.getVarReplaceMap(actionContext));
    }

    public boolean isTasksExistsIfRequired(NbGradleProject project, TaskVariableMap varReplaceMap) {
        NbGradleMultiProjectDef gradleProject = project.getAvailableModel().getProjectDef();
        return isTasksExistsIfRequired(gradleProject, varReplaceMap);
    }

    public boolean isTasksExistsIfRequired(NbGradleMultiProjectDef project, TaskVariableMap varReplaceMap) {
        for (Name name: taskNames) {
            if (name.mustExist) {
                String processedName = StandardTaskVariable.replaceVars(name.getName(), varReplaceMap);
                if (!isTaskExists(project, processedName)) {
                    return false;
                }
            }
        }
        return true;
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
