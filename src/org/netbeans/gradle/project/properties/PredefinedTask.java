package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.project.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbGradleTask;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.tasks.GradleTaskDef;

public final class PredefinedTask {
    public static final String VAR_PROJECT_NAME = "${project}";
    public static final String VAR_TEST_FILE_PATH = "${test-file-path}";

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

    public static Map<String, String> varReplaceMap(NbGradleModule mainModule) {
        String uniqueName = mainModule.getUniqueName();
        if (":".equals(uniqueName)) { // This is the root project.
            uniqueName = "";
        }

        return Collections.singletonMap(VAR_PROJECT_NAME, uniqueName);
    }

    private static String processString(String str, Map<String, String> varReplaceMap) {
        String result = str;
        for (Map.Entry<String, String> entry: varReplaceMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static List<String> processList(List<String> strings, Map<String, String> varReplaceMap) {
        List<String> result = new ArrayList<String>(strings.size());
        for (String str: strings) {
            result.add(processString(str, varReplaceMap));
        }
        return result;
    }

    public GradleTaskDef.Builder createTaskDefBuilder(String caption, NbGradleProject project) {
        return createTaskDefBuilder(caption, varReplaceMap(project.getAvailableModel().getMainModule()));
    }

    public GradleTaskDef.Builder createTaskDefBuilder(String caption, Map<String, String> varReplaceMap) {
        List<String> processedTaskNames = new LinkedList<String>();
        for (Name name: taskNames) {
            processedTaskNames.add(processString(name.getName(), varReplaceMap));
        }

        GradleTaskDef.Builder builder = new GradleTaskDef.Builder(caption, processedTaskNames);
        builder.setArguments(processList(arguments, varReplaceMap));
        builder.setJvmArguments(processList(jvmArguments, varReplaceMap));

        builder.setNonBlocking(nonBlocking);
        builder.setCleanOutput(!nonBlocking);
        builder.setReuseOutput(nonBlocking);

        return builder;
    }

    public GradleTaskDef tryCreateTaskDef(NbGradleProject project) {
        return tryCreateTaskDef(project, varReplaceMap(project.getAvailableModel().getMainModule()));
    }

    public GradleTaskDef tryCreateTaskDef(NbGradleProject project, Map<String, String> varReplaceMap) {
        NbGradleModule mainModule = project.getAvailableModel().getMainModule();

        List<String> processedTaskNames = new LinkedList<String>();
        for (Name name: taskNames) {
            String processName = processString(name.getName(), varReplaceMap);
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
