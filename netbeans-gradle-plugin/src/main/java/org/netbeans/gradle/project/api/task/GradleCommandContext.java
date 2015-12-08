package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.openide.windows.InputOutput;

public final class GradleCommandContext {
    private final Project project;
    private final InputOutput outputTab;

    public GradleCommandContext(@Nonnull Project project, @Nonnull InputOutput outputTab) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(outputTab, "outputTab");

        this.project = project;
        this.outputTab = outputTab;
    }

    public Project getProject() {
        return project;
    }

    public InputOutput getOutputTab() {
        return outputTab;
    }
}
