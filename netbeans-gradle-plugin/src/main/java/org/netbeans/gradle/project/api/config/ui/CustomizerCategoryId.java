package org.netbeans.gradle.project.api.config.ui;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Defines the identification of a project settings page. That is,
 * this method stores the information for the
 * {@link org.netbeans.spi.project.ui.support.ProjectCustomizer.Category#create(String, String, java.awt.Image, org.netbeans.spi.project.ui.support.ProjectCustomizer.Category[]) ProjectCustomizer.Category.create}
 * method.
 * <P>
 * Instances of this class are immutable and as such can be shared without any
 * further synchronization.
 *
 * @see org.netbeans.spi.project.ui.support.ProjectCustomizer.Category#create(String, String, java.awt.Image, org.netbeans.spi.project.ui.support.ProjectCustomizer.Category[]) ProjectCustomizer.Category.create
 * @see ProfileBasedSettingsCategory
 */
public final class CustomizerCategoryId {
    private final String categoryName;
    private final String displayName;

    /**
     * Creates a new {@code CustomizerCategoryId} with the given properties.
     *
     * @param categoryName the programmatic name of the category. This argument
     *   cannot be {@code null}.
     * @param displayName the name to be shown to the user. This argument cannot
     *   be {@code null}.
     */
    public CustomizerCategoryId(@Nonnull String categoryName, @Nonnull String displayName) {
        this.categoryName = Objects.requireNonNull(categoryName, "categoryName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    /**
     * Returns the programmatic name of the category.
     *
     * @return the programmatic name of the category. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Returns the name to be shown to the user in the project properties
     * dialog.
     *
     * @return the name to be shown to the user. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public String getDisplayName() {
        return displayName;
    }
}
