package org.netbeans.gradle.project.api.task;

import java.io.IOException;
import org.netbeans.gradle.project.tasks.EmptyTaskVarMap;

public interface GradleCommandService extends AutoCloseable {
    public static final GradleCommandService NO_SERVICE = new GradleCommandService() {
        @Override
        public TaskVariableMap getTaskVariables() {
            return EmptyTaskVarMap.INSTANCE;
        }

        @Override
        public void close() throws IOException {
        }
    };

    public TaskVariableMap getTaskVariables();

    @Override
    public void close() throws IOException;
}
