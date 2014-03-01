package org.netbeans.gradle.project.properties;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.NbStrings;
import org.openide.awt.HtmlBrowser;
import org.openide.filesystems.FileChooserBuilder;

// !!! Note: This file cannot be renamed, moved or deleted. !!!
// This is required for backward compatibility because GlobalGradleSettings
// relies on this class to store the global settings.
// If this file is ever moved, check GlobalGradleSettings, so that it still
// references the "org.netbeans.gradle.project.properties.GradleSettingsPanel"
// class (and not something else which may happen due to automated refactoring).
@SuppressWarnings("serial")
public class GradleSettingsPanel extends javax.swing.JPanel {
    private static final URL HELP_URL = getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Global-Settings");

    public GradleSettingsPanel() {
        initComponents();

        fillModelLoadStrategyCombo();
        updateSettings();
    }

    private static URL getSafeURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void fillModelLoadStrategyCombo() {
        jModelLoadStrategy.removeAllItems();
        for (ModelLoadingStrategy strategy: ModelLoadingStrategy.values()) {
            jModelLoadStrategy.addItem(new ModelLoadStrategyItem(strategy));
        }
    }

    private void fillPlatformCombo() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        JavaPlatformItem[] comboItems = new JavaPlatformItem[platforms.length];
        for (int i = 0; i < platforms.length; i++) {
            comboItems[i] = new JavaPlatformItem(platforms[i]);
        }

        jJdkCombo.setModel(new DefaultComboBoxModel<>(comboItems));
    }

    public final void updateSettings() {
        fillPlatformCombo();

        jGradlePathEdit.setText(GlobalGradleSettings.getGradleHome().getValueAsString());
        jGradleJVMArgs.setText(GlobalGradleSettings.getGradleJvmArgs().getValueAsString());

        JavaPlatform currentJdk = GlobalGradleSettings.getGradleJdk().getValue();
        if (currentJdk != null) {
            jJdkCombo.setSelectedItem(new JavaPlatformItem(currentJdk));
        }

        jSkipTestsCheck.setSelected(GlobalGradleSettings.getSkipTests().getValue());
        jProjectCacheSize.setValue(GlobalGradleSettings.getProjectCacheSize().getValue());
        jAlwayClearOutput.setSelected(GlobalGradleSettings.getAlwaysClearOutput().getValue());
        jDontAddInitScriptCheck.setSelected(GlobalGradleSettings.getOmitInitScript().getValue());
        jReliableJavaVersionCheck.setSelected(GlobalGradleSettings.getMayRelyOnJavaOfScript().getValue());
        jModelLoadStrategy.setSelectedItem(new ModelLoadStrategyItem(
                GlobalGradleSettings.getModelLoadingStrategy().getValue()));

        File userHome = GlobalGradleSettings.getGradleUserHomeDir().getValue();
        jGradleUserHomeEdit.setText(userHome != null ? userHome.getPath() : "");
    }

    public final void saveSettings() {
        GlobalGradleSettings.getGradleHome().setValueFromString(getGradleInstallDir());
        GlobalGradleSettings.getGradleJvmArgs().setValueFromString(getGradleJvmArgs());
        GlobalGradleSettings.getGradleJdk().setValue(getJdk());
        GlobalGradleSettings.getSkipTests().setValue(jSkipTestsCheck.isSelected());
        GlobalGradleSettings.getProjectCacheSize().setValue(getProjectCacheSize());
        GlobalGradleSettings.getAlwaysClearOutput().setValue(jAlwayClearOutput.isSelected());
        GlobalGradleSettings.getOmitInitScript().setValue(jDontAddInitScriptCheck.isSelected());
        GlobalGradleSettings.getMayRelyOnJavaOfScript().setValue(jReliableJavaVersionCheck.isSelected());
        GlobalGradleSettings.getGradleUserHomeDir().setValueFromString(getGradleUserHomeDir());
        GlobalGradleSettings.getModelLoadingStrategy().setValue(getModelLoadingStrategy());
    }

    private ModelLoadingStrategy getModelLoadingStrategy() {
        ModelLoadStrategyItem selected = (ModelLoadStrategyItem)jModelLoadStrategy.getSelectedItem();
        return selected != null
                ? selected.strategy
                : ModelLoadingStrategy.NEWEST_POSSIBLE;
    }

    private String getGradleUserHomeDir() {
        String result = jGradleUserHomeEdit.getText();
        return result != null ? result.trim() : "";
    }

    private String getGradleInstallDir() {
        String result = jGradlePathEdit.getText();
        return result != null ? result.trim() : "";
    }

    private String getGradleJvmArgs() {
        String result = jGradleJVMArgs.getText();
        return result != null ? result.trim() : "";
    }

    private JavaPlatform getJdk() {
        @SuppressWarnings("unchecked")
        JavaPlatformItem selected = (JavaPlatformItem)jJdkCombo.getSelectedItem();
        return selected != null ? selected.getPlatform() : JavaPlatform.getDefault();
    }

    private int getProjectCacheSize() {
        Object value = jProjectCacheSize.getValue();
        int result;
        if (value instanceof Number) {
            result = ((Number)value).intValue();
        }
        else {
            result = GlobalGradleSettings.getProjectCacheSize().getValue();
        }
        return result > 0 ? result : 1;
    }

    private static class JavaPlatformItem {
        private final JavaPlatform platform;

        public JavaPlatformItem(JavaPlatform platform) {
            if (platform == null) throw new NullPointerException("platform");
            this.platform = platform;
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        @Override
        public String toString() {
            return platform.getDisplayName();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.platform.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final JavaPlatformItem other = (JavaPlatformItem)obj;
            if (this.platform != other.platform && !this.platform.equals(other.platform)) {
                return false;
            }
            return true;
        }
    }

    private static final class ModelLoadStrategyItem {
        public final ModelLoadingStrategy strategy;
        private final String displayName;

        public ModelLoadStrategyItem(ModelLoadingStrategy strategy) {
            this.strategy = strategy;
            this.displayName = NbStrings.getModelLoadStrategy(strategy);
        }

        @Override
        public int hashCode() {
            return 235 + strategy.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final ModelLoadStrategyItem other = (ModelLoadStrategyItem)obj;
            return this.strategy == other.strategy;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jGradlePathCaption = new javax.swing.JLabel();
        jGradlePathEdit = new javax.swing.JTextField();
        jBrowsePathButton = new javax.swing.JButton();
        jGradleVMArgsCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jGradleJVMArgs = new javax.swing.JTextArea();
        jJdkCombo = new javax.swing.JComboBox<JavaPlatformItem>();
        jGradleJdkCaption = new javax.swing.JLabel();
        jSkipTestsCheck = new javax.swing.JCheckBox();
        jProjectCacheSize = new javax.swing.JSpinner();
        jProjectCacheSizeLabel = new javax.swing.JLabel();
        jAlwayClearOutput = new javax.swing.JCheckBox();
        jDontAddInitScriptCheck = new javax.swing.JCheckBox();
        jGradleUserHomeCaption = new javax.swing.JLabel();
        jGradleUserHomeEdit = new javax.swing.JTextField();
        jBrowseUserHomeDirButton = new javax.swing.JButton();
        jReliableJavaVersionCheck = new javax.swing.JCheckBox();
        jModelLoadStrategy = new javax.swing.JComboBox<ModelLoadStrategyItem>();
        jModelLoadStrategyLabel = new javax.swing.JLabel();
        jReadWikiButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jGradlePathCaption, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradlePathCaption.text")); // NOI18N

        jGradlePathEdit.setText(org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradlePathEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jBrowsePathButton, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jBrowsePathButton.text")); // NOI18N
        jBrowsePathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowsePathButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jGradleVMArgsCaption, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradleVMArgsCaption.text")); // NOI18N

        jGradleJVMArgs.setColumns(20);
        jGradleJVMArgs.setRows(3);
        jScrollPane1.setViewportView(jGradleJVMArgs);

        org.openide.awt.Mnemonics.setLocalizedText(jGradleJdkCaption, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradleJdkCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSkipTestsCheck, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jSkipTestsCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jProjectCacheSizeLabel, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jProjectCacheSizeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAlwayClearOutput, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jAlwayClearOutput.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDontAddInitScriptCheck, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jDontAddInitScriptCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleUserHomeCaption, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradleUserHomeCaption.text")); // NOI18N

        jGradleUserHomeEdit.setText(org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradleUserHomeEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jBrowseUserHomeDirButton, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jBrowseUserHomeDirButton.text")); // NOI18N
        jBrowseUserHomeDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowseUserHomeDirButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jReliableJavaVersionCheck, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jReliableJavaVersionCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jModelLoadStrategyLabel, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jModelLoadStrategyLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jReadWikiButton, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jReadWikiButton.text")); // NOI18N
        jReadWikiButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jReadWikiButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jModelLoadStrategyLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jModelLoadStrategy, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jJdkCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jGradleUserHomeEdit, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jGradlePathEdit, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jGradlePathCaption, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jBrowsePathButton)
                            .addComponent(jBrowseUserHomeDirButton, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jGradleVMArgsCaption)
                            .addComponent(jReadWikiButton)
                            .addComponent(jSkipTestsCheck)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jProjectCacheSizeLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jAlwayClearOutput)
                            .addComponent(jDontAddInitScriptCheck)
                            .addComponent(jReliableJavaVersionCheck)
                            .addComponent(jGradleJdkCaption)
                            .addComponent(jGradleUserHomeCaption))
                        .addGap(0, 114, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jReadWikiButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradlePathCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradlePathEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowsePathButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleUserHomeCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradleUserHomeEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowseUserHomeDirButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleJdkCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jJdkCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleVMArgsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jModelLoadStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jModelLoadStrategyLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSkipTestsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jProjectCacheSizeLabel)
                    .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jAlwayClearOutput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jDontAddInitScriptCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jReliableJavaVersionCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBrowsePathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowsePathButtonActionPerformed
        FileChooserBuilder dlgChooser = new FileChooserBuilder(GradleSettingsPanel.class);
        dlgChooser.setDirectoriesOnly(true);
        File f = dlgChooser.showOpenDialog();
        if (f != null && f.isDirectory()) {
            File file = f.getAbsoluteFile();
            jGradlePathEdit.setText(file.toString());
        }
    }//GEN-LAST:event_jBrowsePathButtonActionPerformed

    private void jBrowseUserHomeDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowseUserHomeDirButtonActionPerformed
        FileChooserBuilder dlgChooser = new FileChooserBuilder(GradleSettingsPanel.class);
        dlgChooser.setDirectoriesOnly(true);

        File f = dlgChooser.showOpenDialog();
        if (f != null && f.isDirectory()) {
            File file = f.getAbsoluteFile();
            jGradleUserHomeEdit.setText(file.toString());
        }
    }//GEN-LAST:event_jBrowseUserHomeDirButtonActionPerformed

    private void jReadWikiButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jReadWikiButtonActionPerformed
        HtmlBrowser.URLDisplayer.getDefault().showURLExternal(HELP_URL);
    }//GEN-LAST:event_jReadWikiButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jAlwayClearOutput;
    private javax.swing.JButton jBrowsePathButton;
    private javax.swing.JButton jBrowseUserHomeDirButton;
    private javax.swing.JCheckBox jDontAddInitScriptCheck;
    private javax.swing.JTextArea jGradleJVMArgs;
    private javax.swing.JLabel jGradleJdkCaption;
    private javax.swing.JLabel jGradlePathCaption;
    private javax.swing.JTextField jGradlePathEdit;
    private javax.swing.JLabel jGradleUserHomeCaption;
    private javax.swing.JTextField jGradleUserHomeEdit;
    private javax.swing.JLabel jGradleVMArgsCaption;
    private javax.swing.JComboBox<JavaPlatformItem> jJdkCombo;
    private javax.swing.JComboBox<ModelLoadStrategyItem> jModelLoadStrategy;
    private javax.swing.JLabel jModelLoadStrategyLabel;
    private javax.swing.JSpinner jProjectCacheSize;
    private javax.swing.JLabel jProjectCacheSizeLabel;
    private javax.swing.JButton jReadWikiButton;
    private javax.swing.JCheckBox jReliableJavaVersionCheck;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JCheckBox jSkipTestsCheck;
    // End of variables declaration//GEN-END:variables
}
