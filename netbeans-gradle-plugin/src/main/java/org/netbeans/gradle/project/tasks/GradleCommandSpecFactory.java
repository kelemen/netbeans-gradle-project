package org.netbeans.gradle.project.tasks;

import org.jtrim.cancel.CancellationToken;

public interface GradleCommandSpecFactory {
    public String getDisplayName();
    public GradleCommandSpec tryCreateCommandSpec(CancellationToken cancelToken) throws Exception;
}
