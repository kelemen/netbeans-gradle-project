package org.netbeans.gradle.project.tasks;

public final class DaemonTaskDef {
    private final String caption;
    private final boolean nonBlocking;
    private final DaemonTask task;

    public DaemonTaskDef(String caption, boolean nonBlocking, DaemonTask task) {
        if (caption == null) throw new NullPointerException("caption");
        if (task == null) throw new NullPointerException("task");

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
