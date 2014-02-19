package org.netbeans.gradle.project.api.task;

/**
 * TODO: Document
 *
 * @see CustomCommandActions
 * @see GradleCommandExecutor
 */
public interface CommandExceptionHider {
    /**
     * TODO: Document
     */
    public boolean hideException(Throwable taskError);
}
