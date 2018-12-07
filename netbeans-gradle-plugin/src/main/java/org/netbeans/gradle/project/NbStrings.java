package org.netbeans.gradle.project;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import org.gradle.util.GradleVersion;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.openide.util.NbBundle;

public final class NbStrings {
    public static String getSrcPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SrcJava");
    }

    public static String getResourcesPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Resources");
    }

    public static String getTestPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestJava");
    }

    public static String getTestResourcesPackageCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestResources");
    }

    public static String getOtherPackageCaption(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OtherSources", sourceSetName);
    }

    public static String getMultiRootSourceGroups(String mainName, String subName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.MultiRootSourceGroups", mainName, subName);
    }

    public static String getSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SubProjects");
    }

    public static String getLoadingProjectText(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.LoadingProject", projectName);
    }

    public static String getExecuteTasksText(String taskName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ExecutingGradleTasks", taskName);
    }

    public static String getExecutingTaskMessage(String command) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ExecutingTask", command);
    }

    public static String getTaskArgumentsMessage(List<String> arguments) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TaskArguments", arguments);
    }

    public static String getTaskJvmArgumentsMessage(List<String> arguments) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TaskJvmArguments", arguments);
    }

    public static String getDependenciesNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Dependencies");
    }

    public static String getBuildScriptsNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.BuildScripts");
    }

    public static String getCompileForSourceSet(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CompileForSourceSet", sourceSetName);
    }

    public static String getProvidedForSourceSet(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ProvidedForSourceSet", sourceSetName);
    }

    public static String getRuntimeForSourceSet(String sourceSetName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RuntimeForSourceSet", sourceSetName);
    }

    public static String getSourceSetInherits(String baseName, String inherited) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SourceSetInherits", baseName, inherited);
    }

    private static String removeHotKey(String str) {
        return str.replace("&", "");
    }

    private static String adjustCaption(String str, boolean hotKey) {
        return hotKey ? str : removeHotKey(str);
    }

    public static String getRunCommandCaption(boolean hotKey) {
        return adjustCaption(NbBundle.getMessage(NbStrings.class, "NbStrings.Run"), hotKey);
    }

    public static String getDebugCommandCaption(boolean hotKey) {
        return adjustCaption(NbBundle.getMessage(NbStrings.class, "NbStrings.Debug"), hotKey);
    }

    public static String getBuildCommandCaption(boolean hotKey) {
        return adjustCaption(NbBundle.getMessage(NbStrings.class, "NbStrings.Build"), hotKey);
    }

    public static String getTestCommandCaption(boolean hotKey) {
        return adjustCaption(NbBundle.getMessage(NbStrings.class, "NbStrings.Test"), hotKey);
    }

    public static String getTestWithCoverageCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestWithCoverage");
    }

    public static String getCustomTestsAction() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CustomTestsAction");
    }

    public static String getCleanCommandCaption(boolean hotKey) {
        return adjustCaption(NbBundle.getMessage(NbStrings.class, "NbStrings.Clean"), hotKey);
    }

    public static String getRebuildCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Rebuild");
    }

    public static String getJavadocCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Javadoc");
    }

    public static String getCustomTasksCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CustomTasks");
    }

    public static String getTestSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestSingle");
    }

    public static String getDebugTestSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DebugTestSingle");
    }

    public static String getTestSingleMethodCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestSingleMethod");
    }

    public static String getDebugTestSingleMethodCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DebugTestSingleMethod");
    }

    public static String getRunSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RunSingle");
    }

    public static String getDebugSingleCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DebugSingle");
    }

    public static String getTestSingleMethodAgain() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestSingleMethodAgain");
    }

    public static String getDebugTestSingleMethodAgain() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DebugTestSingleMethodAgain");
    }

    public static String getCompileSingleCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CompileSingle");
    }

    public static String getTestClassAgain() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TestClassAgain");
    }

    public static String getApplyCodeChangesCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ApplyCodeChanges");
    }

    public static String getTasksMenuCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Tasks");
    }

    public static String getSetAsMainCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SetAsMain");
    }

    public static String getReloadCommandCaption(boolean hotKey) {
        return adjustCaption(NbBundle.getMessage(NbStrings.class, "NbStrings.ReloadProject"), hotKey);
    }

    public static String getRefreshNodeCommandCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RefreshNodeCommandCaption");
    }

    public static String getOpenImmediateSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenImmediateSubProjects");
    }

    public static String getBuildSrcNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.BuildSrcNode");
    }

    public static String getOpenBuildSrcCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenBuildSrc");
    }

    public static String getBuildSrcMarker(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.BuildSrcMarker", projectName);
    }

    public static String getOpenSubProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenEverySubProject");
    }

    public static String getOpenRootProjectsCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenRootProjectsCaption");
    }

    public static String getOpenSubProjectCaption(Collection<? extends NbGradleProjectTree> projects) {
        int numberOfProjects = projects.size();
        if (numberOfProjects == 1) {
            String name = projects.iterator().next().getProjectName();
            return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenSingleSubProject", name);
        }
        else {
            return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenMoreSubProject", numberOfProjects);
        }
    }

    public static String getCustomTaskDlgTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CustomTaskTitle");
    }

    public static String getManageCustomTasksTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ManageCustomTasksTitle");
    }

    public static String getManageBuiltInTasksTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ManageBuiltInTasksTitle");
    }

    public static String getAddNewTaskDlgTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.AddNewTaskTitle");
    }

    public static String getProjectPropertiesDlgTitle(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ProjectPropertiesTitle", projectName);
    }

    public static String getInvalidClassPathEntryTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.InvalidClassPathEntryTitle");
    }

    public static String getInvalidClassPathEntry(String entryName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.InvalidClassPathEntry", entryName);
    }

    public static String getErrorCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Error");
    }

    public static String getWarningCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Warning");
    }

    public static String getInfoCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Info");
    }

    public static String getExecuteLabel() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Execute");
    }

    public static String getSaveAndExecuteLabel() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SaveAndExecute");
    }

    public static String getDeleteProjectCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DeleteProject");
    }

    public static String getYesOption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OptionYes");
    }

    public static String getNoOption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OptionNo");
    }

    public static String getOkOption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OptionOk");
    }

    public static String getCancelOption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OptionCancel");
    }

    public static String getTaskVariableQueryCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TaskVariableQueryCaption");
    }

    public static String getConfirmDeleteProject(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmDeleteProject", projectName);
    }

    public static String getConfirmDeleteProjectTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmDeleteProjectTitle");
    }

    public static String getDeleteProjectProgress(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DeleteProjectProgress", projectName);
    }

    public static String getErrorDeleteProject(Throwable error) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ErrorDeleteProject", error);
    }

    public static String getErrorDeleteProjectTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ErrorDeleteProjectTitle");
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

        return NbBundle.getMessage(NbStrings.class, "NbStrings.ErrorLoadingProject", errorText.toString());
    }

    public static String getSelectProjectLocationCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SelectProjectLocation");
    }

    public static String getParsingModel() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ParsingModel");
    }

    public static String getFetchingToolingModel(Class<?> modelType) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.FetchingToolingModel", modelType.getSimpleName());
    }

    public static String getProjectErrorTitle(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ProjectErrorTitle", projectName);
    }

    public static String getProjectLoadFailure(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ProjectLoadFailure", projectName);
    }

    public static String getBuildFailure(String tasks) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.BuildFailure", tasks);
    }

    public static String getGradleTaskFailure() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleTaskFailure");
    }

    public static String getDefaultProfileName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DefaultProfile");
    }

    public static String getGlobalProfileName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GlobalProfile");
    }

    public static String getAddNewProfileCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.AddNewProfileCaption");
    }

    public static String getInvalidProfileName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.InvalidProfileName");
    }

    public static String getEmptyProfileName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.EmptyProfileName");
    }

    public static String getConfirmRemoveProfile(String profileName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmRemoveProfile", profileName);
    }

    public static String getUserHomeFileName(String fileName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.UserHomeFile", fileName);
    }

    public static String getRootProjectMarker(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RootProjectMarker", projectName);
    }

    public static String getBuildSrcDescription(String projectName, String projectPath) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.BuildSrcDescription", projectName, projectPath);
    }

    public static String getRootProjectDescription(String projectName, String projectPath) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RootProjectDescription", projectName, projectPath);
    }

    public static String getSubProjectDescription(String projectName, String projectPath) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SubProjectDescription", projectName, projectPath);
    }

    public static String getGradleProjectCategoryName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleProjectCategoryName");
    }

    public static String getGradleProjectLicenseCategoryName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleProjectLicenseCategoryName");
    }

    public static String getAppearanceCategoryName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.AppearanceCategoryName");
    }

    public static String getCustomVariablesCategoryName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CustomVariablesCategoryName");
    }

    public static String getDownloadSourcesProgressCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DownloadSourcesProgressCaption");
    }

    public static String getDownloadSources() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DownloadSources");
    }

    public static String getDownloadSourcesFailure() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DownloadSourcesFailure");
    }

    public static String getWebPages() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.WebAppDir");
    }

    public static String getReRunName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ReRunName");
    }

    public static String getReRunDescription() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ReRunDescription");
    }

    public static String getStopTaskCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.StopTaskCaption");
    }

    public static String getStopTaskDescription() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.StopTaskDescription");
    }

    public static String getConfirmStopTaskTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmStopTaskTitle");
    }

    public static String getConfirmStopTask(String taskName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmStopTask", taskName);
    }

    public static String getReRunDiffName() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ReRunDiffName");
    }

    public static String getReRunDiffDescription() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ReRunDiffDescription");
    }

    public static String getEnumDisplayValue(Enum<?> value) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings." + value.getClass().getSimpleName() + "." + value.name());
    }

    public static String getJavaSourcesDisplayMode(JavaSourcesDisplayMode displayMode) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.JavaSourcesDisplayMode." + displayMode.name());
    }

    public static String getGlobalErrorReporterTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GlobalErrorReporterTitle");
    }

    public static String getTooSmallCache(int prevCacheSize, int newCacheSize) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.TooSmallCache", prevCacheSize, newCacheSize);
    }

    public static String getCachedJarIssueMessage() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CachedJarIssueMessage");
    }

    public static String getIssueWithGradle18Message(String gradleVersion) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.IssueWithGradle18Message", gradleVersion);
    }

    public static String getIssueWithGradle23Message(String gradleVersion) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.IssueWithGradle23Message", gradleVersion);
    }

    public static String getCreateSourceDirsAction() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CreateSourceDirsAction");
    }

    public static String getDeleteEmptySourceDirsAction() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DeleteEmptySourceDirsAction");
    }

    public static String getSourceDirsActionGroup() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SourceDirsActionGroup");
    }

    public static String getDependencyResolutionFailure(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.DependencyResolutionFailure", projectName);
    }

    public static String getRuntimeDependencyResolutionFailure(String projectName, String sourceSet) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RuntimeDependencyResolutionFailure", projectName, sourceSet);
    }

    public static String getCompileDependencyResolutionFailure(String projectName, String sourceSet) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CompileDependencyResolutionFailure", projectName, sourceSet);
    }

    public static String getShowStackTrace() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ShowStackTrace");
    }

    public static String getNeedsMinGradleVersion(GradleVersion version) {
        GradleVersion baseVersion = version.getBaseVersion();
        return NbBundle.getMessage(NbStrings.class, "NbStrings.NeedsMinGradleVersion", baseVersion.getVersion());
    }

    public static String getCoreGradlePlugin() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CoreGradlePlugin");
    }

    public static String getInternalExtensionErrorInProject(String extensionName, String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.InternalExtensionErrorInProject",
                extensionName, projectName);
    }

    public static String getBuildScriptErrorInProject(String projectName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.BuildScriptErrorInProject",
                projectName);
    }

    public static String getErrorDueToNoTestReportsFound(String testTaskName, File testReportDir) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ErrorDueToNoTestReportsFound",
                testTaskName,
                testReportDir);
    }

    public static String getJumpToSource() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.JumpToSource");
    }

    public static String getLoading() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Loading");
    }

    public static String getGradleLocationDlgTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleLocationDlgTitle");
    }

    public static String getGradleLocationDefault() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleLocation.DEFAULT");
    }

    public static String getGradleLocationVersion(String version) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleLocation.VERSION", version);
    }

    public static String getGradleLocationDist(URI location) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleLocation.DIST", location);
    }

    public static String getGradleLocationLocal(File dir) {
        String dirStr = dir != null ? dir.getPath() : NbStrings.getInvalidMark();
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleLocation.LOCAL", dirStr);
    }

    public static String getCustomNamePatternLabel() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CustomNamePatternLabel");
    }

    public static String getSettingsCategoryGradleInstallation() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.GradleInstallation");
    }

    public static String getSettingsCategoryPlatformPriority() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.PlatformPriority");
    }

    public static String getSettingsCategoryScriptAndTasks() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.ScriptAndTasks");
    }

    public static String getSettingsCategoryScript() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.Script");
    }

    public static String getSettingsCategoryTasks() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.Tasks");
    }

    public static String getSettingsCategoryJavaModules() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.JavaModules");
    }

    public static String getSettingsCategoryDebugJava() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.DebugJava");
    }

    public static String getSettingsCategoryOther() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.Other");
    }

    public static String getSettingsCategoryDaemon() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.Daemon");
    }

    public static String getSettingsCategoryAppearance() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.SettingsCategory.Appearance");
    }

    public static String getProjectScriptNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ProjectScriptNodeCaption");
    }

    public static String getRootProjectScriptNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.RootProjectScriptNodeCaption");
    }

    public static String getScanForChangesCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ScanForChangesCaption");
    }

    public static String getGradleHomeNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GradleHomeNodeCaption");
    }

    public static String getGlobalInitScriptsNodeCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.GlobalInitScriptsNodeCaption");
    }

    public static String getOpenFileCaption(String baseName) {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.OpenFileCaption", baseName);
    }

    public static String getConfirmCreateBuildSrcMessage() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmCreateBuildSrcMessage");
    }

    public static String getConfirmCreateBuildSrcTitle() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.ConfirmCreateBuildSrcTitle");
    }

    public static String getCreateInitDDir() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.CreateInitDDir");
    }

    public static String getAddNewInitScriptCaption() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.AddNewInitScriptCaption");
    }

    public static String getInvalidMark() {
        return NbBundle.getMessage(NbStrings.class, "NbStrings.Invalid");
    }

    private NbStrings() {
        throw new AssertionError();
    }
}
