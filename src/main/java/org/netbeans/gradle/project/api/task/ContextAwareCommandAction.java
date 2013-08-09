package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 *
 */
public interface ContextAwareCommandAction {
    /***/
    @Nonnull
    public ContextAwareCommandFinalizer startCommand(@Nonnull Project project, @Nonnull Lookup commandContext);
}
