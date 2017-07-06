package org.netbeans.gradle.project.tasks;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;

public final class DaemonTaskDef {
    private final String caption;
    private final boolean nonBlocking;
    private final DaemonTask task;

    public DaemonTaskDef(String caption, boolean nonBlocking, DaemonTask task) {
        this.caption = Objects.requireNonNull(caption, "caption");
        this.nonBlocking = nonBlocking;
        this.task = Objects.requireNonNull(task, "task");
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

    public DaemonTaskDefFactory toFactory() {
        return new ConstFactory(this);
    }

    private static class ConstFactory implements DaemonTaskDefFactory {
        private final DaemonTaskDef taskDef;

        public ConstFactory(DaemonTaskDef taskDef) {
            this.taskDef = taskDef;
        }

        @Override
        public String getDisplayName() {
            return taskDef.getCaption();
        }

        @Override
        public DaemonTaskDef tryCreateTaskDef(CancellationToken cancelToken) {
            return taskDef;
        }
    }
}
