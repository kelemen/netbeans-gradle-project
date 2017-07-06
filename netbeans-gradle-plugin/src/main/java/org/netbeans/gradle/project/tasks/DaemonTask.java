package org.netbeans.gradle.project.tasks;

import org.jtrim2.cancel.CancellationToken;
import org.netbeans.api.progress.ProgressHandle;

public interface DaemonTask {
    public void run(CancellationToken cancelToken, ProgressHandle progress);
}
