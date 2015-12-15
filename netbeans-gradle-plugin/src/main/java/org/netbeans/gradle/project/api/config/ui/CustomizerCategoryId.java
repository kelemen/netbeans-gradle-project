package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;

public final class CustomizerCategoryId {
    private final String categoryName;
    private final String displayName;

    public CustomizerCategoryId(@Nonnull String categoryName, @Nonnull String displayName) {
        ExceptionHelper.checkNotNullArgument(categoryName, "categoryName");
        ExceptionHelper.checkNotNullArgument(displayName, "displayName");

        this.categoryName = categoryName;
        this.displayName = displayName;
    }

    @Nonnull
    public String getCategoryName() {
        return categoryName;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }
}
