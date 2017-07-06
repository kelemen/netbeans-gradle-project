package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.model.NbGradleMultiProjectDef;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.openide.util.Lookup;

public final class PredefinedTask {
    public static final class Name {
        private final String name;
        private final boolean mustExist;

        public Name(String name, boolean mustExist) {
            this.name = Objects.requireNonNull(name, "name");
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

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + Objects.hashCode(this.name);
            hash = 43 * hash + (this.mustExist ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final Name other = (Name)obj;
            return Objects.equals(this.name, other.name)
                    && this.mustExist == other.mustExist;
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
        this.displayName = Objects.requireNonNull(displayName, "displayName");
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
        List<String> rawTaskNames = new ArrayList<>(taskNames.size());
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
        StringResolver taskNameResolver = StringResolvers
                .getDefaultResolverSelector()
                .getProjectResolver(project, actionContext);
        return isTasksExistsIfRequired(project, taskNameResolver);
    }

    public boolean isTasksExistsIfRequired(NbGradleProject project, StringResolver taskNameResolver) {
        NbGradleMultiProjectDef gradleProject = project.currentModel().getValue().getProjectDef();
        return isTasksExistsIfRequired(gradleProject, taskNameResolver);
    }

    public boolean isTasksExistsIfRequired(NbGradleMultiProjectDef project, StringResolver taskNameResolver) {
        for (Name name: taskNames) {
            if (name.mustExist) {
                String processedName = taskNameResolver.resolveString(name.getName());
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.displayName);
        hash = 17 * hash + Objects.hashCode(this.taskNames);
        hash = 17 * hash + Objects.hashCode(this.arguments);
        hash = 17 * hash + Objects.hashCode(this.jvmArguments);
        hash = 17 * hash + (this.nonBlocking ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final PredefinedTask other = (PredefinedTask)obj;
        return Objects.equals(this.displayName, other.displayName)
                && Objects.equals(this.taskNames, other.taskNames)
                && Objects.equals(this.taskNames, other.taskNames)
                && Objects.equals(this.arguments, other.arguments)
                && Objects.equals(this.jvmArguments, other.jvmArguments)
                && this.nonBlocking == other.nonBlocking;
    }
}
