package org.netbeans.gradle.project.tasks;

import org.jtrim2.cancel.CancellationToken;

public interface GradleTaskDefFactory {
    public String getDisplayName();
    public GradleTaskDef tryCreateTaskDef(CancellationToken cancelToken) throws Exception;
}
