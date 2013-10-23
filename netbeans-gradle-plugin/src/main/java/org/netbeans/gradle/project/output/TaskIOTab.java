package org.netbeans.gradle.project.output;

import org.openide.windows.InputOutput;

// TODO: Add ReRun, etc... button handlers
public final class TaskIOTab implements IOTabDef {
    private final InputOutputWrapper io;

    public TaskIOTab(InputOutput io) {
        this.io = new InputOutputWrapper(io);
    }

    public InputOutputWrapper getIo() {
        return io;
    }

    @Override
    public boolean isClosed() {
        return io.getIo().isClosed();
    }
}
