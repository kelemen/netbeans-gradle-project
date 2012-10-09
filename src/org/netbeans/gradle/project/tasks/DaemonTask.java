package org.netbeans.gradle.project.tasks;

import org.netbeans.api.progress.ProgressHandle;

public interface DaemonTask {
    public void run(ProgressHandle progress) throws Exception;
}
