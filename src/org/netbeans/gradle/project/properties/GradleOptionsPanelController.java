package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

@OptionsPanelController.SubRegistration(
        location = "Advanced",
        displayName = "#AdvancedOption_DisplayName_Gradle",
        keywords = "#AdvancedOption_Keywords_Gradle",
        keywordsCategory = "Advanced/Gradle")
public final class GradleOptionsPanelController extends OptionsPanelController {
    // Fall-back to this variable if no set.
    private static final String GRADLE_HOME_ENV_VARIABLE = "GRADLE_HOME";
    private static final String GRADLE_HOME_PROPERTY_NAME = "gradle-home";

    private GradleSettingsPanel panel;

    private GradleSettingsPanel getPanel() {
        if (panel == null) {
            panel = new GradleSettingsPanel();
        }
        return panel;
    }

    private static Preferences getPreferences() {
        return NbPreferences.forModule(GradleSettingsPanel.class);
    }

    public static void addSettingsChangeListener(PreferenceChangeListener listener) {
        getPreferences().addPreferenceChangeListener(listener);
    }

    public static void removeSettingsChangeListener(PreferenceChangeListener listener) {
        getPreferences().removePreferenceChangeListener(listener);
    }

    public static String getGradleHome() {
        String gradleHome = getPreferences().get(GRADLE_HOME_PROPERTY_NAME, "");
        if (gradleHome.isEmpty()) {
            gradleHome = System.getenv(GRADLE_HOME_ENV_VARIABLE);
            gradleHome = gradleHome != null ? gradleHome.trim() : "";
        }
        return gradleHome;
    }

    public void save() {
        getPreferences().put(GRADLE_HOME_PROPERTY_NAME, getPanel().getGradleHome());
    }

    @Override
    public void update() {
        getPanel().updateSettings();
    }

    @Override
    public void applyChanges() {
        NbPreferences.forModule(GradleSettingsPanel.class)
                .put(GRADLE_HOME_PROPERTY_NAME, getPanel().getGradleHome());
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isChanged() {
        return true;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
}
