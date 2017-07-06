package org.netbeans.gradle.project.tasks;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.cancel.CancellationToken;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleActionProviderContext;
import org.netbeans.gradle.project.api.task.GradleCommandExecutor;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.openide.util.Lookup;

public final class DefaultGradleCommandExecutor implements GradleCommandExecutor {
    private final NbGradleProject project;

    public DefaultGradleCommandExecutor(NbGradleProject project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    @Override
    public void executeCommand(
            final GradleCommandTemplate command,
            final CustomCommandActions customActions) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(customActions, "customActions");

        GradleTaskDefFactory taskDefFactory = new GradleTaskDefFactory() {
            @Override
            public String getDisplayName() {
                return command.getSafeDisplayName();
            }

            @Override
            public GradleTaskDef tryCreateTaskDef(CancellationToken cancelToken) {
                return GradleTaskDef.createFromTemplate(project, command, customActions, Lookup.EMPTY).create();
            }
        };

        Set<GradleActionProviderContext> actionContexts = Collections.emptySet();

        Runnable asyncTask = GradleTasks.createAsyncGradleTask(project, taskDefFactory, actionContexts, (Throwable error) -> {
            try {
                CommandCompleteListener completeListener = customActions.getCommandCompleteListener();
                if (completeListener != null) {
                    completeListener.onComplete(error);
                }
            } finally {
                GradleTasks.projectTaskCompleteListener(project).onComplete(error);
            }
        });
        asyncTask.run();
    }
}
