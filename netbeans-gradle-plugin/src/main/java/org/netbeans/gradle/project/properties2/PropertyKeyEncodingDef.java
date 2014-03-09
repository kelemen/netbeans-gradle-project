package org.netbeans.gradle.project.properties2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface PropertyKeyEncodingDef<ValueKey> {
    @Nullable
    public ValueKey decode(@Nonnull ConfigTree config);

    @Nonnull
    public ConfigTree encode(@Nonnull ValueKey value);
}
