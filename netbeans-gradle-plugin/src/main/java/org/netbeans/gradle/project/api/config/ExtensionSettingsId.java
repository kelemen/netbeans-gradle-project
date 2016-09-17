package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;

public final class ExtensionSettingsId {
    private final String id;

    public ExtensionSettingsId(@Nonnull String id) {
        ExceptionHelper.checkNotNullArgument(id, "id");

        this.id = id;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ExtensionSettingsId{" + id + '}';
    }
}
