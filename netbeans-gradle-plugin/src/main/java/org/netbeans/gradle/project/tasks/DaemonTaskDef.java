package org.netbeans.gradle.project.tasks;

import org.jtrim.utils.ExceptionHelper;

public final class DaemonTaskDef {
    private final String caption;
    private final boolean nonBlocking;
    private final DaemonTask task;

    public DaemonTaskDef(String caption, boolean nonBlocking, DaemonTask task) {
        ExceptionHelper.checkNotNullArgument(caption, "caption");
        ExceptionHelper.checkNotNullArgument(task, "task");

        this.caption = caption;
        this.nonBlocking = nonBlocking;
        this.task = task;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isNonBlocking() {
        return nonBlocking;
    }

    public DaemonTask getTask() {
        return task;
    }
}
