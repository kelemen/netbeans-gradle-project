package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.List;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
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

    public static String getOtherPackageCaption(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_OtherSources", sourceSetName);
    }

    public static String getMultiRootSourceGroups(String mainName, String subName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_MultiRootSourceGroups", mainName, subName);
    }

    public static String getSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_SubProjects");
    }

    public static String getLoadingProjectText(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_LoadingProject", projectName);
    }

    public static String getExecuteTasksText(String taskName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ExecutingGradleTasks", taskName);
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

    public static String getBuildScriptsNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_BuildScripts");
    }

    public static String getCompileForSourceSet(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_CompileForSourceSet", sourceSetName);
    }

    public static String getProvidedForSourceSet(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ProvidedForSourceSet", sourceSetName);
    }

    public static String getRuntimeForSourceSet(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_RuntimeForSourceSet", sourceSetName);
    }

    public static String getSourceSetInherits(String baseName, String inherited) {
        return NbBundle.getMessage(NbStrings.class, "LBL_SourceSetInherits", baseName, inherited);
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

    public static String getTestSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_TestSingle");
    }

    public static String getDebugTestSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DebugTestSingle");
    }

    public static String getRunSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_RunSingle");
    }

    public static String getDebugSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DebugSingle");
    }

    public static String getApplyCodeChangesCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ApplyCodeChanges");
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

    public static String getBuildSrcNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_BuildSrcNode");
    }

    public static String getOpenBuildSrcCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OpenBuildSrc");
    }

    public static String getBuildSrcMarker(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_BuildSrcMarker", projectName);
    }

    public static String getOpenSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OpenEverySubProject");
    }

    public static String getOpenSubProjectCaption(Collection<? extends NbGradleProjectTree> projects) {
        int numberOfProjects = projects.size();
        if (numberOfProjects == 1) {
            String name = projects.iterator().next().getProjectName();
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

    public static String getManageBuiltInTasksDlgTitle(String profile) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ManageBuiltInTasksTitle", profile);
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

    public static String getDeleteProjectCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DeleteProject");
    }

    public static String getYesOption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OptionYes");
    }

    public static String getNoOption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OptionNo");
    }

    public static String getOkOption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OptionOk");
    }

    public static String getCancelOption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_OptionCancel");
    }

    public static String getTaskVariableQueryCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_TaskVariableQueryCaption");
    }

    public static String getConfirmDeleteProject(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "MSG_ConfirmDeleteProject", projectName);
    }

    public static String getConfirmDeleteProjectTitle() {
        return NbBundle.getMessage(NbStrings.class, "MSG_ConfirmDeleteProjectTitle");
    }

    public static String getDeleteProjectProgress(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_DeleteProjectProgress", projectName);
    }

    public static String getErrorDeleteProject(Throwable error) {
        return NbBundle.getMessage(NbStrings.class, "MSG_ErrorDeleteProject", error);
    }

    public static String getErrorDeleteProjectTitle() {
        return NbBundle.getMessage(NbStrings.class, "MSG_ErrorDeleteProjectTitle");
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
        return NbBundle.getMessage(NbStrings.class, "LBL_ProjectErrorTitle", projectName);
    }

    public static String getProjectLoadFailure(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "MSG_ProjectLoadFailure", projectName);
    }

    public static String getBuildFailure(String tasks) {
        return NbBundle.getMessage(NbStrings.class, "MSG_BuildFailure", tasks);
    }

    public static String getGradleTaskFailure() {
        return NbBundle.getMessage(NbStrings.class, "MSG_GradleTaskFailure");
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

    public static String getUserHomeFileName(String fileName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_UserHomeFile", fileName);
    }

    public static String getRootProjectMarker(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "LBL_RootProjectMarker", projectName);
    }

    public static String getRootProjectDescription(String projectName, String projectPath) {
        return NbBundle.getMessage(NbStrings.class, "HINT_RootProjectMarker", projectName, projectPath);
    }

    public static String getSubProjectDescription(String projectName, String projectPath) {
        return NbBundle.getMessage(NbStrings.class, "HINT_SubProjectMarker", projectName, projectPath);
    }

    public static String getGradleProjectCategoryName() {
        return NbBundle.getMessage(NbStrings.class, "LBL_GradleProjectCategoryName");
    }

    public static String getGradleProjectLicenseCategoryName() {
        return NbBundle.getMessage(NbStrings.class, "LBL_GradleProjectLicenseCategoryName");
    }

    public static String getDownloadSourcesProgressCaption() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DownloadSourcesProgressCaption");
    }

    public static String getDownloadSources() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DownloadSources");
    }

    public static String getDownloadSourcesFailure() {
        return NbBundle.getMessage(NbStrings.class, "LBL_DownloadSourcesFailure");
    }

    public static String getWebPages() {
        return NbBundle.getMessage(NbStrings.class, "LBL_WebAppDir");
    }

    public static String getReRunName() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ReRunName");
    }

    public static String getReRunDescription() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ReRunDescription");
    }

    public static String getReRunDiffName() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ReRunDiffName");
    }

    public static String getReRunDiffDescription() {
        return NbBundle.getMessage(NbStrings.class, "LBL_ReRunDiffDescription");
    }

    public static String getModelLoadStrategy(ModelLoadingStrategy strategy) {
        return NbBundle.getMessage(NbStrings.class, "LBL_ModelLoadStrategy." + strategy.name());
    }

    private NbStrings() {
        throw new AssertionError();
    }
}
