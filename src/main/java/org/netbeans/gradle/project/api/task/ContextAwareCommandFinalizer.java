package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.openide.windows.OutputWriter;

/**
 *
 */
public interface ContextAwareCommandFinalizer {
    /***/
    public void finalizeSuccessfulCommand(
            @Nonnull OutputWriter output,
            @Nonnull OutputWriter errOutput);
}
