package org.netbeans.gradle.project.tasks;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;

public final class GradleTasks {
    private static final Logger LOGGER = Logger.getLogger(GradleTasks.class.getName());

    private static Callable<GradleCommandSpec> toSpecFactory(final Callable<GradleTaskDef> taskDefFactory) {
        if (taskDefFactory == null) throw new NullPointerException("taskDefFactory");

        return new Callable<GradleCommandSpec>() {
            @Override
            public GradleCommandSpec call() throws Exception {
                GradleTaskDef result = taskDefFactory.call();
                return result != null
                        ? new GradleCommandSpec(result, null)
                        : null;
            }
        };
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            Callable<GradleTaskDef> taskDefFactory) {
        return createAsyncGradleTaskFromSpec(project, toSpecFactory(taskDefFactory));
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            Callable<GradleTaskDef> taskDefFactory,
            CommandCompleteListener listener) {
        return createAsyncGradleTaskFromSpec(project, toSpecFactory(taskDefFactory), listener);
    }

    public static Runnable createAsyncGradleTaskFromSpec(
            NbGradleProject project,
            Callable<GradleCommandSpec> taskDefFactory) {
        return new AsyncGradleTask(project, taskDefFactory, projectTaskCompleteListener(project));
    }

    public static Runnable createAsyncGradleTaskFromSpec(
            NbGradleProject project,
            Callable<GradleCommandSpec> taskDefFactory,
            CommandCompleteListener listener) {
        return new AsyncGradleTask(project, taskDefFactory, listener);
    }

    public static CommandCompleteListener projectTaskCompleteListener(final NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        return new CommandCompleteListener() {
            @Override
            public void onComplete(Throwable error) {
                if (error != null) {
                    LOGGER.log(error instanceof Exception ? Level.INFO : Level.SEVERE,
                            "Gradle build failure.",
                            error);

                    String buildFailureMessage = NbStrings.getGradleTaskFailure();
                    project.displayError(buildFailureMessage, error, false);
                }
            }
        };
    }

    private GradleTasks() {
        throw new AssertionError();
    }
}
