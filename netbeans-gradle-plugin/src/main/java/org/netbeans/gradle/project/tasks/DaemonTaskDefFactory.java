package org.netbeans.gradle.project.tasks;

import org.jtrim2.cancel.CancellationToken;

public interface DaemonTaskDefFactory {
    public String getDisplayName();
    public DaemonTaskDef tryCreateTaskDef(CancellationToken cancelToken) throws Exception;
}
