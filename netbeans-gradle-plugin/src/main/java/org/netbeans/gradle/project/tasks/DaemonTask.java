package org.netbeans.gradle.project.tasks;

import org.jtrim.cancel.CancellationToken;
import org.netbeans.api.progress.ProgressHandle;

public interface DaemonTask {
    public void run(CancellationToken cancelToken, ProgressHandle progress);
}
