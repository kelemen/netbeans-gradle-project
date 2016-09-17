package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;

public final class ProfileBasedSettingsCategory {
    private final CustomizerCategoryId categoryId;
    private final ProfileBasedSettingsPageFactory settingsPageFactory;

    public ProfileBasedSettingsCategory(
            @Nonnull CustomizerCategoryId categoryId,
            @Nonnull ProfileBasedSettingsPageFactory settingsPageFactory) {
        ExceptionHelper.checkNotNullArgument(categoryId, "categoryId");
        ExceptionHelper.checkNotNullArgument(settingsPageFactory, "settingsPageFactory");

        this.categoryId = categoryId;
        this.settingsPageFactory = settingsPageFactory;
    }

    @Nonnull
    public CustomizerCategoryId getCategoryId() {
        return categoryId;
    }

    @Nonnull
    public ProfileBasedSettingsPageFactory getSettingsPageFactory() {
        return settingsPageFactory;
    }
}
