package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.gradle.project.tasks.vars.StringResolver;

public interface GradleLocationRef {
    @Nonnull
    public String getUniqueTypeName();

    @Nullable
    public String asString();

    @Nonnull
    public GradleLocation getLocation(@Nonnull StringResolver resolver);
}
