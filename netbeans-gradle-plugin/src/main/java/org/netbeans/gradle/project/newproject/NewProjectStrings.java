package org.netbeans.gradle.project.newproject;

import org.openide.util.NbBundle;

public final class NewProjectStrings {
    public static String getInvalidPath() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_InvalidPath");
    }

    public static String getInvalidFileName() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_InvalidFileName");
    }

    public static String getDirectoryAlreadyExists() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_DirectoryAlreadyExists");
    }

    public static String getCannotCreateFolderHere() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_CannotCreateFolderHere");
    }

    public static String getNotRecommendedProjectName() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_NotRecommendedProjectName");
    }

    public static String getIllegalProjectName() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_IllegalProjectName");
    }

    public static String getIllegalIdentifier() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_IllegalIdentifier");
    }

    public static String getShouldNotUseDefaultPackage() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_ShouldNotUseDefaultPackage");
    }

    public static String getProjectNameMustNotBeEmpty() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_ProjectNameMustNotBeEmpty");
    }

    public static String getInvalidGroupId() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_InvalidGroupId");
    }

    public static String getInvalidVersion() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_InvalidVersion");
    }

    public static String getNotRootProject() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_NotRootProject");
    }

    public static String getTargetFolderNotAvailable() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_TargetFolderNotAvailable");
    }

    public static String getFileNameRequired() {
        return NbBundle.getMessage(NewProjectStrings.class, "MSG_FileNameRequired");
    }

    private NewProjectStrings() {
        throw new AssertionError();
    }
}
