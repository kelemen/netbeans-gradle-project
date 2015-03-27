package org.netbeans.gradle.project.properties.global;

import java.net.URL;
import javax.swing.JComponent;
import org.jtrim.property.PropertySource;

public interface GlobalSettingsEditor {
    public void updateSettings();
    public void saveSettings();
    public PropertySource<Boolean> valid();
    public URL getHelpUrl();

    public JComponent getEditorComponent();
}
