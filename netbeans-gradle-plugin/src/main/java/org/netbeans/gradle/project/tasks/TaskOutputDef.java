package org.netbeans.gradle.project.tasks;

public final class TaskOutputDef {
    private final TaskOutputKey key;
    private final String caption;

    public TaskOutputDef(TaskOutputKey key, String caption) {
        if (key == null) throw new NullPointerException("key");
        if (caption == null) throw new NullPointerException("caption");

        this.key = key;
        this.caption = caption;
    }

    public TaskOutputKey getKey() {
        return key;
    }

    public String getCaption() {
        return caption;
    }
}
