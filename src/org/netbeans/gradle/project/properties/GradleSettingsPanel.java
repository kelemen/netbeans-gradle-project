package org.netbeans.gradle.project.properties;

import java.io.File;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.openide.filesystems.FileChooserBuilder;

// !!! Note: This file cannot be renamed, moved or deleted. !!!
// This is required for backward compatibility because GlobalGradleSettings
// relies on this class to store the global settings.
// If this file is ever moved, check GlobalGradleSettings, so that it still
// references the "org.netbeans.gradle.project.properties.GradleSettingsPanel"
// class (and not something else which may happen due to automated refactoring).
@SuppressWarnings("serial")
public class GradleSettingsPanel extends javax.swing.JPanel {
    public GradleSettingsPanel() {
        initComponents();

        updateSettings();
    }

    private void fillPlatformCombo() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        JavaPlatformItem[] comboItems = new JavaPlatformItem[platforms.length];
        for (int i = 0; i < platforms.length; i++) {
            comboItems[i] = new JavaPlatformItem(platforms[i]);
        }

        jJdkCombo.setModel(new DefaultComboBoxModel(comboItems));
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
    }

    public final void saveSettings() {
        GlobalGradleSettings.getGradleHome().setValueFromString(getGradleHome());
        GlobalGradleSettings.getGradleJvmArgs().setValueFromString(getGradleJvmArgs());
        GlobalGradleSettings.getGradleJdk().setValue(getJdk());
        GlobalGradleSettings.getSkipTests().setValue(isSkipTests());
        GlobalGradleSettings.getProjectCacheSize().setValue(getProjectCacheSize());
        GlobalGradleSettings.getAlwaysClearOutput().setValue(isAlwaysClearOutput());
        GlobalGradleSettings.getOmitInitScript().setValue(isDontAddInitScript());
    }

    private String getGradleHome() {
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

    private boolean isSkipTests() {
        return jSkipTestsCheck.isSelected();
    }

    private boolean isAlwaysClearOutput() {
        return jAlwayClearOutput.isSelected();
    }

    private boolean isDontAddInitScript() {
        return jDontAddInitScriptCheck.isSelected();
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
        jJdkCombo = new javax.swing.JComboBox();
        jGradleJdkCaption = new javax.swing.JLabel();
        jSkipTestsCheck = new javax.swing.JCheckBox();
        jProjectCacheSize = new javax.swing.JSpinner();
        jProjectCacheSizeLabel = new javax.swing.JLabel();
        jAlwayClearOutput = new javax.swing.JCheckBox();
        jDontAddInitScriptCheck = new javax.swing.JCheckBox();

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
        jGradleJVMArgs.setRows(5);
        jScrollPane1.setViewportView(jGradleJVMArgs);

        org.openide.awt.Mnemonics.setLocalizedText(jGradleJdkCaption, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jGradleJdkCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSkipTestsCheck, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jSkipTestsCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jProjectCacheSizeLabel, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jProjectCacheSizeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAlwayClearOutput, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jAlwayClearOutput.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDontAddInitScriptCheck, org.openide.util.NbBundle.getMessage(GradleSettingsPanel.class, "GradleSettingsPanel.jDontAddInitScriptCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jJdkCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jGradlePathEdit, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jGradlePathCaption, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBrowsePathButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jDontAddInitScriptCheck)
                            .addComponent(jSkipTestsCheck)
                            .addComponent(jGradleJdkCaption)
                            .addComponent(jGradleVMArgsCaption)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jProjectCacheSizeLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jAlwayClearOutput))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jGradlePathCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradlePathEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowsePathButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleJdkCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jJdkCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jGradleVMArgsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addContainerGap(29, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBrowsePathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowsePathButtonActionPerformed
        FileChooserBuilder dlgChooser = new FileChooserBuilder(GradleSettingsPanel.class);
        File f = dlgChooser.showOpenDialog();
        if (f != null && f.isDirectory()) {
            File file = f.getAbsoluteFile();
            jGradlePathEdit.setText(file.toString());
        }
    }//GEN-LAST:event_jBrowsePathButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jAlwayClearOutput;
    private javax.swing.JButton jBrowsePathButton;
    private javax.swing.JCheckBox jDontAddInitScriptCheck;
    private javax.swing.JTextArea jGradleJVMArgs;
    private javax.swing.JLabel jGradleJdkCaption;
    private javax.swing.JLabel jGradlePathCaption;
    private javax.swing.JTextField jGradlePathEdit;
    private javax.swing.JLabel jGradleVMArgsCaption;
    private javax.swing.JComboBox jJdkCombo;
    private javax.swing.JSpinner jProjectCacheSize;
    private javax.swing.JLabel jProjectCacheSizeLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JCheckBox jSkipTestsCheck;
    // End of variables declaration//GEN-END:variables
}
