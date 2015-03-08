package org.netbeans.gradle.project.api.task;

/**
 * Defines context objects which might be passed to the
 * {@link org.netbeans.spi.project.ActionProvider} of this plugin to
 * alter its behaviour.
 */
public enum GradleActionProviderContext {
    /**
     * If the action provider sees this object in its context, it won't
     * save files before executing the task. This is useful if you are
     * executing a command from an OnSave task.
     */
    DONT_SAVE_FILES,

    /**
     * If the action provider sees this object in its context, it won't
     * move the output window to the front.
     */
    DONT_FOCUS_ON_OUTPUT,
}
