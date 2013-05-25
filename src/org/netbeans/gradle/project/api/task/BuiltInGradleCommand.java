package org.netbeans.gradle.project.api.task;

public interface BuiltInGradleCommand {
    public GradleCommandTemplate getDefaultGradleCommand();
    public CommandCompleteListener getAfterCommandAction();
}
