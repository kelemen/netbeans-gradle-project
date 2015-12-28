package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a profile based project property editor page. Instances of these
 * classes are usually created by a {@link ProfileBasedProjectSettingsPageFactory}.
 *
 * @see ProfileBasedConfigurations
 * @see ProfileBasedProjectSettingsPageFactory
 */
public final class ProfileBasedProjectSettingsPage {
    private final JComponent settingsPanel;
    private final ProfileValuesEditorFactory editorFactory;

    /**
     * Creates a new {@code ProfileBasedProjectSettingsPage} with the
     * given <I>Swing</I> component and the logic of saving and updating
     * properties on this component.
     *
     * @param settingsPanel the <I>Swing</I> component displaying the editors
     *   of the properties to be adjusted. This argument cannot be {@code null}.
     * @param editorFactory the logic of saving and updating properties edited
     *   on this page. This argument cannot be {@code null}.
     */
    public ProfileBasedProjectSettingsPage(@Nonnull JComponent settingsPanel, @Nonnull ProfileValuesEditorFactory editorFactory) {
        ExceptionHelper.checkNotNullArgument(settingsPanel, "settingsPanel");
        ExceptionHelper.checkNotNullArgument(editorFactory, "editorFactory");

        this.settingsPanel = settingsPanel;
        this.editorFactory = editorFactory;
    }

    /**
     * Returns the <I>Swing</I> component displaying the editors of the
     * properties to be adjusted.
     *
     * @return the <I>Swing</I> component displaying the editors of the
     *   properties to be adjusted. This method never returns {@code null}.
     */
    @Nonnull
    public JComponent getSettingsPanel() {
        return settingsPanel;
    }

    /**
     * Returns the logic of saving and updating properties edited on this page.
     *
     * @return the logic of saving and updating properties edited on this page.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public ProfileValuesEditorFactory getEditorFactory() {
        return editorFactory;
    }
}
