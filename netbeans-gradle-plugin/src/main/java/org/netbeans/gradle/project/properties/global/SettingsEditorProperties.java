package org.netbeans.gradle.project.properties.global;

import java.net.URL;
import javax.swing.JComponent;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

public final class SettingsEditorProperties {
    public static final class Builder {
        private final JComponent editorComponent;
        private PropertySource<Boolean> valid;
        private URL helpUrl;

        public Builder(JComponent editorComponent) {
            ExceptionHelper.checkNotNullArgument(editorComponent, "editorComponent");

            this.editorComponent = editorComponent;
            this.valid = PropertyFactory.constSource(true);
            this.helpUrl = null;
        }

        public void setValid(PropertySource<Boolean> valid) {
            ExceptionHelper.checkNotNullArgument(valid, "valid");

            this.valid = valid;
        }

        public void setHelpUrl(URL helpUrl) {
            this.helpUrl = helpUrl;
        }

        public SettingsEditorProperties create() {
            return new SettingsEditorProperties(this);
        }
    }

    private final JComponent editorComponent;
    private final PropertySource<Boolean> valid;
    private final URL helpUrl;

    private SettingsEditorProperties(Builder builder) {
        this.editorComponent = builder.editorComponent;
        this.valid = builder.valid;
        this.helpUrl = builder.helpUrl;
    }

    public JComponent getEditorComponent() {
        return editorComponent;
    }

    public PropertySource<Boolean> valid() {
        return valid;
    }

    public URL getHelpUrl() {
        return helpUrl;
    }
}
