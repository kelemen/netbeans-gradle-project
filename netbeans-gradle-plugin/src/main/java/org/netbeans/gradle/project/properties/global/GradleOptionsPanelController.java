package org.netbeans.gradle.project.properties.global;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
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
            settingsPanel = new GlobalGradleSettingsPanel();
            update();
        }
        return settingsPanel;
    }

    private static ActiveSettingsQuery getSettings() {
        return CommonGlobalSettings.getDefaultActiveSettingsQuery();
    }

    private ProfileEditor getEditor() {
        ProfileEditor result = profileEditor;
        if (result == null) {
            ProfileInfo profileInfo = new ProfileInfo(ProfileKey.GLOBAL_PROFILE, NbStrings.getGlobalProfileName());
            result = getPanel().startEditingProfile(profileInfo, getSettings());
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
