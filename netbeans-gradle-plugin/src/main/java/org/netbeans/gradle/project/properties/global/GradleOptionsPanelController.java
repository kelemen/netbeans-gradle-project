package org.netbeans.gradle.project.properties.global;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.properties.ProfileEditor;
import org.netbeans.gradle.project.properties.ProfileInfo;
import org.netbeans.gradle.project.properties.ui.GlobalGradleSettingsPanel;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.SubRegistration(
        location = "Advanced",
        displayName = "#AdvancedOption_DisplayName_Gradle",
        keywords = "#AdvancedOption_Keywords_Gradle",
        keywordsCategory = "Advanced/Gradle")
public final class GradleOptionsPanelController extends OptionsPanelController {
    private GlobalGradleSettingsPanel settingsPanel;
    private ProfileEditor profileEditor;

    private GlobalGradleSettingsPanel getPanel() {
        if (settingsPanel == null) {
            settingsPanel = new GlobalGradleSettingsPanel(getSettings());
            update();
        }
        return settingsPanel;
    }

    private ActiveSettingsQuery getSettings() {
        return CommonGlobalSettings.getDefault().getActiveSettingsQuery();
    }

    private ProfileEditor getEditor() {
        ProfileEditor result = profileEditor;
        if (result == null) {
            result = getPanel().startEditingProfile(
                    new ProfileInfo(ProfileKey.GLOBAL_PROFILE, "Global settings"),
                    getSettings());
        }
        return result;
    }

    @Override
    public void update() {
        ProfileEditor editor = getEditor();
        editor.readFromSettings().displaySettings();
    }

    @Override
    public void applyChanges() {
        ProfileEditor editor = getEditor();
        editor.readFromGui().saveSettings();
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
