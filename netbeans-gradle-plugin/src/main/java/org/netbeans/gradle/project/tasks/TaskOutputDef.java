package org.netbeans.gradle.project.tasks;

import org.jtrim.utils.ExceptionHelper;

public final class TaskOutputDef {
    private final TaskOutputKey key;
    private final String caption;

    public TaskOutputDef(TaskOutputKey key, String caption) {
        ExceptionHelper.checkNotNullArgument(key, "key");
        ExceptionHelper.checkNotNullArgument(caption, "caption");

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
