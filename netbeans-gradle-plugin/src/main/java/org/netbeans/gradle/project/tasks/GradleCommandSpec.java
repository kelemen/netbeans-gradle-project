package org.netbeans.gradle.project.tasks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class GradleCommandSpec {
    private final GradleTaskDef source;
    private final GradleTaskDef processed;

    public GradleCommandSpec(GradleTaskDef source, GradleTaskDef processed) {
        if (source == null) throw new NullPointerException("source");

        this.source = source;
        this.processed = processed;
    }

    @Nonnull
    public GradleTaskDef getSource() {
        return source;
    }

    @Nullable
    public GradleTaskDef getProcessed() {
        return processed;
    }
}
