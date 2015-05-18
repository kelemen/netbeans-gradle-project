package org.netbeans.gradle.project.api.config;

import java.util.List;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.task.DaemonTaskContext;

public interface GradleArgumentQuery {
    @Nonnull
    public List<String> getExtraArgs(@Nonnull DaemonTaskContext context);

    @Nonnull
    public List<String> getExtraJvmArgs(@Nonnull DaemonTaskContext context);
}
