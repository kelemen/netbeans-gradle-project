package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import javax.swing.JCheckBox;
import javax.swing.SpinnerNumberModel;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbFileUtils;

@SuppressWarnings("serial")
public class OtherOptionsPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Other");

    public OtherOptionsPanel() {
        initComponents();

        jProjectCacheSize.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    }

    public static GlobalSettingsPage createSettingsPage() {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new OtherOptionsPanel());
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private void displayCheck(JCheckBox checkbox, Boolean value, PropertyReference<Boolean> propertyRef) {
        displayCheck(checkbox, value != null ? value : propertyRef.getActiveValue());
    }

    private void displayCheck(JCheckBox checkbox, Boolean value) {
        if (value != null) {
            checkbox.setSelected(value);
        }
    }

    private void displayProjectCacheSize(Integer value) {
        if (value != null) {
            jProjectCacheSize.setValue(value);
        }
    }

    private int getProjectCacheSize(PropertyRefs properties) {
        Object value = jProjectCacheSize.getValue();
        int result;
        if (value instanceof Number) {
            result = ((Number)value).intValue();
        }
        else {
            result = properties.projectCacheSizeRef.getActiveValue();
        }
        return result > 0 ? result : 1;
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<Boolean> detectProjectDependenciesByJarNameRef;
        private final PropertyReference<Boolean> compileOnSaveRef;
        private final PropertyReference<Integer> projectCacheSizeRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            detectProjectDependenciesByJarNameRef = CommonGlobalSettings.detectProjectDependenciesByJarName(settingsQuery);
            compileOnSaveRef = CommonGlobalSettings.compileOnSave(settingsQuery);
            projectCacheSizeRef = CommonGlobalSettings.projectCacheSize(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, OtherOptionsPanel.this);
        }
    }

    private class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final Boolean detectProjectDependenciesByJarName;
        private final Boolean compileOnSave;
        private final Integer projectCacheSize;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.detectProjectDependenciesByJarName = properties.detectProjectDependenciesByJarNameRef.tryGetValueWithoutFallback();
            this.compileOnSave = properties.compileOnSaveRef.tryGetValueWithoutFallback();
            this.projectCacheSize = properties.projectCacheSizeRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, OtherOptionsPanel panel) {
            this.properties = properties;
            this.detectProjectDependenciesByJarName = panel.jDetectProjectDependenciesByName.isSelected();
            this.compileOnSave = panel.jCompileOnSaveCheckbox.isSelected();
            this.projectCacheSize = panel.getProjectCacheSize(properties);
        }

        @Override
        public void displaySettings() {
            displayCheck(jDetectProjectDependenciesByName, detectProjectDependenciesByJarName, properties.detectProjectDependenciesByJarNameRef);
            displayCheck(jCompileOnSaveCheckbox, compileOnSave, properties.compileOnSaveRef);
            displayProjectCacheSize(projectCacheSize != null
                    ? projectCacheSize
                    : properties.projectCacheSizeRef.getActiveValue());
        }

        @Override
        public void saveSettings() {
            properties.detectProjectDependenciesByJarNameRef.setValue(detectProjectDependenciesByJarName);
            properties.compileOnSaveRef.setValue(compileOnSave);
            properties.projectCacheSizeRef.setValue(projectCacheSize);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCompileOnSaveCheckbox = new javax.swing.JCheckBox();
        jProjectCacheSizeLabel = new javax.swing.JLabel();
        jProjectCacheSize = new javax.swing.JSpinner();
        jDetectProjectDependenciesByName = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jCompileOnSaveCheckbox, org.openide.util.NbBundle.getMessage(OtherOptionsPanel.class, "OtherOptionsPanel.jCompileOnSaveCheckbox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jProjectCacheSizeLabel, org.openide.util.NbBundle.getMessage(OtherOptionsPanel.class, "OtherOptionsPanel.jProjectCacheSizeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDetectProjectDependenciesByName, org.openide.util.NbBundle.getMessage(OtherOptionsPanel.class, "OtherOptionsPanel.jDetectProjectDependenciesByName.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jProjectCacheSizeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCompileOnSaveCheckbox)
                    .addComponent(jDetectProjectDependenciesByName))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jProjectCacheSizeLabel)
                    .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCompileOnSaveCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jDetectProjectDependenciesByName)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCompileOnSaveCheckbox;
    private javax.swing.JCheckBox jDetectProjectDependenciesByName;
    private javax.swing.JSpinner jProjectCacheSize;
    private javax.swing.JLabel jProjectCacheSizeLabel;
    // End of variables declaration//GEN-END:variables
}
