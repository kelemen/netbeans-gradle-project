package org.netbeans.gradle.model.util;

import org.gradle.tooling.ProjectConnection;

public interface ProjectConnectionTask {
    public void doTask(ProjectConnection connection) throws Exception;
}
