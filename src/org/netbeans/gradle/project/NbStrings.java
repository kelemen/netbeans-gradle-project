package org.netbeans.gradle.project;

import java.util.Arrays;
import java.util.Collection;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.openide.util.NbBundle;

public final class NbStrings {
    public static String getSrcPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_SrcJava");
    }

    public static String getResourcesPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Resources");
    }

    public static String getTestPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_TestJava");
    }

    public static String getTestResourcesPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_TestResources");
    }

    public static String getSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_SubProjects");
    }

    public static String getLoadingProjectText(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_LoadingProject", projectName);
    }

    public static String getExecuteTasksText() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ExecutingGradleTasks");
    }

    public static String getExecutingTaskMessage(String command) {
        return NbBundle.getMessage(NbStrings.class, "MSG_ExecutingTask", command);
    }

    private static String formatArgumentsOfMessage(String... arguments) {
        return Arrays.toString(arguments);
    }

    public static String getTaskArgumentsMessage(String... arguments) {
        String formatted = formatArgumentsOfMessage(arguments);
        return NbBundle.getMessage(NbStrings.class, "MSG_TaskArguments", formatted);
    }

    public static String getTaskJvmArgumentsMessage(String... arguments) {
        String formatted = formatArgumentsOfMessage(arguments);
        return NbBundle.getMessage(NbStrings.class, "MSG_TaskJvmArguments", formatted);
    }

    public static String getDependenciesNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Dependencies");
    }

    public static String getCompileDependenciesNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_CompileDependencies");
    }

    public static String getRuntimeDependenciesNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_RuntimeDependencies");
    }

    public static String getTestCompileDependenciesNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_TestCompileDependencies");
    }

    public static String getTestRuntimeDependenciesNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_TestRuntimeDependencies");
    }

    public static String getRunCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Run");
    }

    public static String getDebugCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Debug");
    }

    public static String getBuildCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Build");
    }

    public static String getCleanCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Clean");
    }

    public static String getRebuildCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Rebuild");
    }

    public static String getJavadocCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Javadoc");
    }

    public static String getCustomTasksCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_CustomTasks");
    }

    public static String getTasksMenuCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Tasks");
    }

    public static String getReloadCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ReloadProject");
    }

    public static String getOpenSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OpenEverySubProject");
    }

    public static String getOpenSubProjectCaption(Collection<NbGradleModule> projects) {
        int numberOfProjects = projects.size();
        if (numberOfProjects == 1) {
            String name = projects.iterator().next().getName();
            return NbBundle.getMessage(NbStrings.class, "LBL_OpenSingleSubProject", name);
        }
        else {
            return NbBundle.getMessage(NbStrings.class, "LBL_OpenMoreSubProject", numberOfProjects);
        }
    }

    public static String getCustomTaskDlgTitle() {
        return NbBundle.getMessage(NbStrings.class, "LBL_CustomTaskTitle");
    }

    public static String getProjectPropertiesDlgTitle(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ProjectPropertiesTitle", projectName);
    }

    public static String getInvalidClassPathEntry(String entryName) {
        return NbBundle.getMessage(NbStrings.class, "MSG_InvalidClassPathEntry", entryName);
    }

    private NbStrings() {
        throw new AssertionError();
    }
}
