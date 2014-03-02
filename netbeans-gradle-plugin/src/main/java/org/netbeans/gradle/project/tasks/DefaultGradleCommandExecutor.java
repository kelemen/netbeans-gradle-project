package org.netbeans.gradle.project.tasks;

import java.util.concurrent.Callable;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandExecutor;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.openide.util.Lookup;

public final class DefaultGradleCommandExecutor implements GradleCommandExecutor {
    private final NbGradleProject project;

    public DefaultGradleCommandExecutor(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    @Override
    public void executeCommand(
            final GradleCommandTemplate command,
            final CustomCommandActions customActions) {
        ExceptionHelper.checkNotNullArgument(command, "command");
        ExceptionHelper.checkNotNullArgument(customActions, "customActions");

        Runnable asyncTask = GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return GradleTaskDef.createFromTemplate(project, command, customActions, Lookup.EMPTY).create();
            }
        }, new CommandCompleteListener() {
            @Override
            public void onComplete(Throwable error) {
                try {
                    CommandCompleteListener completeListener = customActions.getCommandCompleteListener();
                    if (completeListener != null) {
                        completeListener.onComplete(error);
                    }
                } finally {
                    GradleTasks.projectTaskCompleteListener(project).onComplete(error);
                }
            }
        });
        asyncTask.run();
    }
}
