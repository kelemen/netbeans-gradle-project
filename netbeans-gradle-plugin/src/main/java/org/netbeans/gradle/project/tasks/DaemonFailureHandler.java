package org.netbeans.gradle.project.tasks;

public interface DaemonFailureHandler {
    public boolean tryHandleFailure(Throwable failure);
}
