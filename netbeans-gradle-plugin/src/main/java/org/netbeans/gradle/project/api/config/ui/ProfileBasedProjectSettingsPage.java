package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;

public final class ProfileBasedProjectSettingsPage {
    private final JComponent settingsPanel;
    private final ProfileValuesEditorFactory editorFactory;

    public ProfileBasedProjectSettingsPage(@Nonnull JComponent settingsPanel, @Nonnull ProfileValuesEditorFactory editorFactory) {
        ExceptionHelper.checkNotNullArgument(settingsPanel, "settingsPanel");
        ExceptionHelper.checkNotNullArgument(editorFactory, "editorFactory");

        this.settingsPanel = settingsPanel;
        this.editorFactory = editorFactory;
    }

    @Nonnull
    public JComponent getSettingsPanel() {
        return settingsPanel;
    }

    @Nonnull
    public ProfileValuesEditorFactory getEditorFactory() {
        return editorFactory;
    }
}
