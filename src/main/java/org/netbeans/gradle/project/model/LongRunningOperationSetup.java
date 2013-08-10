package org.netbeans.gradle.project.model;

import org.gradle.tooling.LongRunningOperation;

public interface LongRunningOperationSetup {
    public void setupLongRunningOperation(LongRunningOperation op);
}
