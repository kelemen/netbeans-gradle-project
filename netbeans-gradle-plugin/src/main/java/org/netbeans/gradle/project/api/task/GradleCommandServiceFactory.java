package org.netbeans.gradle.project.api.task;

import java.io.IOException;
import javax.annotation.Nonnull;

public interface GradleCommandServiceFactory {
    public static final GradleCommandServiceFactory NO_SERVICE = new GradleCommandServiceFactory() {
        @Override
        public boolean isServiceTaskVariable(TaskVariable variable) {
            return false;
        }

        @Override
        public GradleCommandService startService(GradleCommandContext context) throws IOException {
            return GradleCommandService.NO_SERVICE;
        }
    };

    public boolean isServiceTaskVariable(@Nonnull TaskVariable variable);
    public GradleCommandService startService(GradleCommandContext context) throws IOException;
}
