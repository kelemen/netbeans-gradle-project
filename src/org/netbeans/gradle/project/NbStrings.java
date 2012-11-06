package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.List;
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

    public static String getExecuteTasksText(List<String> taskNames) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ExecutingGradleTasks", taskNames);
    }

    public static String getExecutingTaskMessage(String command) {
        return NbBundle.getMessage(NbStrings.class, "MSG_ExecutingTask", command);
    }

    public static String getTaskArgumentsMessage(List<String> arguments) {
        return NbBundle.getMessage(NbStrings.class, "MSG_TaskArguments", arguments);
    }

    public static String getTaskJvmArgumentsMessage(List<String> arguments) {
        return NbBundle.getMessage(NbStrings.class, "MSG_TaskJvmArguments", arguments);
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

    public static String getTestCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Test");
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

    public static String getOpenImmediateSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OpenImmediateSubProjects");
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

    public static String getManageTasksDlgTitle(String profile) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ManageTasksTitle", profile);
    }

    public static String getAddNewTaskDlgTitle() {
        return NbBundle.getMessage(NbStrings.class, "LBL_AddNewTaskTitle");
    }

    public static String getProjectPropertiesDlgTitle(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ProjectPropertiesTitle", projectName);
    }

    public static String getInvalidClassPathEntry(String entryName) {
        return NbBundle.getMessage(NbStrings.class, "MSG_InvalidClassPathEntry", entryName);
    }

    public static String getErrorCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Error");
    }

    public static String getWarningCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Warning");
    }

    public static String getInfoCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Info");
    }

    public static String getExecuteLabel() {
        return NbBundle.getMessage(NbStrings.class, "LBL_Execute");
    }

    public static String getSaveAndExecuteLabel() {
        return NbBundle.getMessage(NbStrings.class, "LBL_SaveAndExecute");
    }

    public static String getErrorLoadingProject(Throwable ex) {
        StringBuilder errorText = new StringBuilder(1024);
        errorText.append(ex.toString());

        Throwable currentError = ex.getCause();
        while (currentError != null) {
            errorText.append("<br>");
            errorText.append(currentError.toString());
            currentError = currentError.getCause();
        }

        return NbBundle.getMessage(NbStrings.class, "MSG_ErrorLoadingProject", errorText.toString());
    }

    public static String getSelectProjectLocationCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_SelectProjectLocation");
    }

    public static String getParsingModel() {
        return NbBundle.getMessage(NbStrings.class, "MSG_ParsingModel");
    }

    public static String getProjectErrorTitle(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_PROJECT_ERROR_TITLE", projectName);
    }

    public static String getProjectLoadFailure(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "MSG_PROJECT_LOAD_FAILURE", projectName);
    }

    public static String getBuildFailure(String tasks) {
        return NbBundle.getMessage(NbStrings.class, "MSG_BUILD_FAILURE", tasks);
    }

    public static String getDefaultProfileName() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DefaultProfile");
    }

    public static String getAddNewProfileCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_AddNewProfileCaption");
    }

    public static String getInvalidProfileName() {
        return NbBundle.getMessage(NbStrings.class, "MSG_InvalidProfileName");
    }

    public static String getEmptyProfileName() {
        return NbBundle.getMessage(NbStrings.class, "MSG_EmptyProfileName");
    }

    public static String getConfirmRemoveProfile(String profileName) {
        return NbBundle.getMessage(NbStrings.class, "MSG_ConfirmRemoveProfile", profileName);
    }

    private NbStrings() {
        throw new AssertionError();
    }
}
