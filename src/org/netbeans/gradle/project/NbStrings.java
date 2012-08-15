package org.netbeans.gradle.project;

import java.util.Arrays;
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

    public static String getCustomTaskDlgTitle() {
        return NbBundle.getMessage(NbStrings.class, "LBL_CustomTaskTitle");
    }

    private NbStrings() {
        throw new AssertionError();
    }
}
