package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import javax.swing.JCheckBox;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbFileUtils;

@SuppressWarnings("serial")
public class BuildScriptParsingPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Build-Script-Parsing");

    private final EnumCombo<ModelLoadingStrategy> modelLoadingStrategyCombo;

    public BuildScriptParsingPanel() {
        initComponents();

        modelLoadingStrategyCombo = new EnumCombo<>(ModelLoadingStrategy.class, ModelLoadingStrategy.NEWEST_POSSIBLE, jModelLoadStrategy);
    }

    public static GlobalSettingsPage createSettingsPage() {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new BuildScriptParsingPanel());
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private void displayModelLoadingStrategy(ModelLoadingStrategy value) {
        if (value != null) {
            modelLoadingStrategyCombo.setSelectedValue(value);
        }
    }

    private void displayCheck(JCheckBox checkbox, Boolean value, PropertyReference<Boolean> propertyRef) {
        displayCheck(checkbox, value != null ? value : propertyRef.getActiveValue());
    }

    private void displayCheck(JCheckBox checkbox, Boolean value) {
        if (value != null) {
            checkbox.setSelected(value);
        }
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<ModelLoadingStrategy> modelLoadingStrategyRef;
        private final PropertyReference<Boolean> loadRootProjectFirstRef;
        private final PropertyReference<Boolean> mayRelyOnJavaOfScriptRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            modelLoadingStrategyRef = CommonGlobalSettings.modelLoadingStrategy(settingsQuery);
            loadRootProjectFirstRef = CommonGlobalSettings.loadRootProjectFirst(settingsQuery);
            mayRelyOnJavaOfScriptRef = CommonGlobalSettings.mayRelyOnJavaOfScript(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, BuildScriptParsingPanel.this);
        }
    }

    private class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final ModelLoadingStrategy modelLoadingStrategy;
        private final Boolean loadRootProjectFirst;
        private final Boolean mayRelyOnJavaOfScript;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;

            this.modelLoadingStrategy = properties.modelLoadingStrategyRef.tryGetValueWithoutFallback();
            this.loadRootProjectFirst = properties.loadRootProjectFirstRef.tryGetValueWithoutFallback();
            this.mayRelyOnJavaOfScript = properties.mayRelyOnJavaOfScriptRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, BuildScriptParsingPanel panel) {
            this.properties = properties;

            this.modelLoadingStrategy = panel.modelLoadingStrategyCombo.getSelectedValue();
            this.loadRootProjectFirst = panel.jLoadRootProjectFirst.isSelected();
            this.mayRelyOnJavaOfScript = panel.jReliableJavaVersionCheck.isSelected();
        }

        @Override
        public void displaySettings() {
            displayModelLoadingStrategy(modelLoadingStrategy != null
                    ? modelLoadingStrategy
                    : properties.modelLoadingStrategyRef.getActiveValue());
            displayCheck(jLoadRootProjectFirst, loadRootProjectFirst, properties.loadRootProjectFirstRef);
            displayCheck(jReliableJavaVersionCheck, mayRelyOnJavaOfScript, properties.mayRelyOnJavaOfScriptRef);
        }

        @Override
        public void saveSettings() {
            properties.modelLoadingStrategyRef.setValue(modelLoadingStrategy);
            properties.loadRootProjectFirstRef.setValue(loadRootProjectFirst);
            properties.mayRelyOnJavaOfScriptRef.setValue(mayRelyOnJavaOfScript);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jModelLoadStrategy = new javax.swing.JComboBox<>();
        jModelLoadStrategyLabel = new javax.swing.JLabel();
        jReliableJavaVersionCheck = new javax.swing.JCheckBox();
        jLoadRootProjectFirst = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jModelLoadStrategyLabel, org.openide.util.NbBundle.getMessage(BuildScriptParsingPanel.class, "BuildScriptParsingPanel.jModelLoadStrategyLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jReliableJavaVersionCheck, org.openide.util.NbBundle.getMessage(BuildScriptParsingPanel.class, "BuildScriptParsingPanel.jReliableJavaVersionCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLoadRootProjectFirst, org.openide.util.NbBundle.getMessage(BuildScriptParsingPanel.class, "BuildScriptParsingPanel.jLoadRootProjectFirst.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jModelLoadStrategyLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jModelLoadStrategy, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jReliableJavaVersionCheck)
                    .addComponent(jLoadRootProjectFirst))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jModelLoadStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jModelLoadStrategyLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLoadRootProjectFirst)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jReliableJavaVersionCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jLoadRootProjectFirst;
    private javax.swing.JComboBox<EnumCombo.Item<ModelLoadingStrategy>> jModelLoadStrategy;
    private javax.swing.JLabel jModelLoadStrategyLabel;
    private javax.swing.JCheckBox jReliableJavaVersionCheck;
    // End of variables declaration//GEN-END:variables
}
