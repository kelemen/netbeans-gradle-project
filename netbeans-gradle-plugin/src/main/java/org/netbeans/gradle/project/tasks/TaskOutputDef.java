package org.netbeans.gradle.project.tasks;

import java.util.Objects;

public final class TaskOutputDef {
    private final TaskOutputKey key;
    private final String caption;

    public TaskOutputDef(TaskOutputKey key, String caption) {
        this.key = Objects.requireNonNull(key, "key");
        this.caption = Objects.requireNonNull(caption, "caption");
    }

    public TaskOutputKey getKey() {
        return key;
    }

    public String getCaption() {
        return caption;
    }
}
