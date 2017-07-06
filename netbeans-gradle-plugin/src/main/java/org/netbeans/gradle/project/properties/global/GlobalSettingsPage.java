package org.netbeans.gradle.project.properties.global;

import java.net.URL;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;

public final class GlobalSettingsPage extends ProfileBasedSettingsPage {
    private final PropertySource<Boolean> valid;
    private final URL helpUrl;

    private GlobalSettingsPage(Builder builder) {
        super(builder.editorComponent, builder.editorFactory);

        this.valid = builder.valid;
        this.helpUrl = builder.helpUrl;
    }

    @Nonnull
    public PropertySource<Boolean> valid() {
        return valid;
    }

    @Nullable
    public URL getHelpUrl() {
        return helpUrl;
    }

    public static final class Builder {
        private final JComponent editorComponent;
        private final ProfileEditorFactory editorFactory;
        private PropertySource<Boolean> valid;
        private URL helpUrl;

        public <T extends JComponent & ProfileEditorFactory> Builder(T editor) {
            this(editor, editor);
        }

        public Builder(JComponent editorComponent, ProfileEditorFactory editorFactory) {
            Objects.requireNonNull(editorComponent, "editorComponent");
            Objects.requireNonNull(editorFactory, "editorFactory");

            this.editorComponent = editorComponent;
            this.editorFactory = editorFactory;
            this.valid = PropertyFactory.constSource(true);
            this.helpUrl = null;
        }

        public void setValid(PropertySource<Boolean> valid) {
            this.valid = Objects.requireNonNull(valid, "valid");
        }

        public void setHelpUrl(URL helpUrl) {
            this.helpUrl = helpUrl;
        }

        public GlobalSettingsPage create() {
            return new GlobalSettingsPage(this);
        }
    }
}
