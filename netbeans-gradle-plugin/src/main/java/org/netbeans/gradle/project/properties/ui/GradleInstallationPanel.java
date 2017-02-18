package org.netbeans.gradle.project.properties.ui;

import java.io.File;
import java.net.URL;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationRef;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.openide.filesystems.FileChooserBuilder;

@SuppressWarnings("serial")
public class GradleInstallationPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Gradle-Installation");

    private final StringResolver locationResolver;
    private GradleLocationRef selectedGradleLocationRef;

    public GradleInstallationPanel() {
        selectedGradleLocationRef = null;
        locationResolver = StringResolvers.getDefaultGlobalResolver();

        initComponents();
    }

    public static GlobalSettingsPage createSettingsPage() {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new GradleInstallationPanel());
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    private void displayLocationDef(GradleLocationDef locationDef) {
        if (locationDef != null) {
            selectGradleLocation(locationDef.getLocationRef());
            jPreferWrapperCheck.setSelected(locationDef.isPreferWrapper());
        }
        else {
            selectGradleLocation(null);
            jPreferWrapperCheck.setSelected(false);
        }
    }

    private void displayUserHome(File userHome) {
        jGradleUserHomeEdit.setText(userHome != null ? userHome.getPath() : "");
    }

    private String toString(GradleLocationRef locationRef) {
        if (locationRef == null) {
            return "";
        }

        return locationRef.getLocation(locationResolver).toLocalizedString();
    }

    private void selectGradleLocation(GradleLocationRef newLocationRef) {
        selectedGradleLocationRef = newLocationRef;
        jGradleLocationDescription.setText(toString(newLocationRef));
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private GradleLocationDef getGradleLocationDef() {
        if (selectedGradleLocationRef == null) {
            return null;
        }

        return new GradleLocationDef(selectedGradleLocationRef, jPreferWrapperCheck.isSelected());
    }

    private File getGradleUserHomeDir() {
        String result = jGradleUserHomeEdit.getText();
        if (result == null) {
            return null;
        }

        result = result.trim();
        return result.isEmpty() ? null : new File(result);
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<GradleLocationDef> gradleLocationRef;
        private final PropertyReference<File> gradleUserHomeDirRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            gradleLocationRef = CommonGlobalSettings.gradleLocation(settingsQuery);
            gradleUserHomeDirRef = CommonGlobalSettings.gradleUserHomeDir(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, GradleInstallationPanel.this);
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;
        private final GradleLocationDef locationDef;
        private final File userHome;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.locationDef = properties.gradleLocationRef.tryGetValueWithoutFallback();
            this.userHome = properties.gradleUserHomeDirRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, GradleInstallationPanel panel) {
            this.properties = properties;
            this.locationDef = panel.getGradleLocationDef();
            this.userHome = panel.getGradleUserHomeDir();
        }

        @Override
        public void displaySettings() {
            displayLocationDef(locationDef != null
                    ? locationDef
                    : properties.gradleLocationRef.getActiveValue());
            displayUserHome(userHome != null
                    ? userHome
                    : properties.gradleUserHomeDirRef.getActiveValue());
        }

        @Override
        public void saveSettings() {
            properties.gradleLocationRef.setValue(locationDef);
            properties.gradleUserHomeDirRef.setValue(userHome);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jGradlePathCaption = new javax.swing.JLabel();
        jGradleLocationDescription = new javax.swing.JTextField();
        jChangeGradleLocationButton = new javax.swing.JButton();
        jGradleUserHomeCaption = new javax.swing.JLabel();
        jGradleUserHomeEdit = new javax.swing.JTextField();
        jBrowseUserHomeDirButton = new javax.swing.JButton();
        jPreferWrapperCheck = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jGradlePathCaption, org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jGradlePathCaption.text")); // NOI18N

        jGradleLocationDescription.setEditable(false);
        jGradleLocationDescription.setText(org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jGradleLocationDescription.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jChangeGradleLocationButton, org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jChangeGradleLocationButton.text")); // NOI18N
        jChangeGradleLocationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jChangeGradleLocationButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jGradleUserHomeCaption, org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jGradleUserHomeCaption.text")); // NOI18N

        jGradleUserHomeEdit.setText(org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jGradleUserHomeEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jBrowseUserHomeDirButton, org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jBrowseUserHomeDirButton.text")); // NOI18N
        jBrowseUserHomeDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowseUserHomeDirButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jPreferWrapperCheck, org.openide.util.NbBundle.getMessage(GradleInstallationPanel.class, "GradleInstallationPanel.jPreferWrapperCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jGradleUserHomeEdit, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jGradlePathCaption, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(8, 8, 8)
                        .addComponent(jBrowseUserHomeDirButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jGradleUserHomeCaption)
                        .addGap(0, 109, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jChangeGradleLocationButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jGradleLocationDescription)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPreferWrapperCheck)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jGradlePathCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradleLocationDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jChangeGradleLocationButton)
                    .addComponent(jPreferWrapperCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleUserHomeCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradleUserHomeEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowseUserHomeDirButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jChangeGradleLocationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jChangeGradleLocationButtonActionPerformed
        GradleLocationRef currentLocationRef = selectedGradleLocationRef;
        GradleLocationRef newLocationRef = GradleLocationPanel.tryChooseLocation(this, locationResolver, currentLocationRef);
        if (newLocationRef != null) {
            selectGradleLocation(newLocationRef);
        }
    }//GEN-LAST:event_jChangeGradleLocationButtonActionPerformed

    private void jBrowseUserHomeDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowseUserHomeDirButtonActionPerformed
        FileChooserBuilder dlgChooser = new FileChooserBuilder(GradleInstallationPanel.class);
        dlgChooser.setDirectoriesOnly(true);

        File f = dlgChooser.showOpenDialog();
        if (f != null && f.isDirectory()) {
            File file = f.getAbsoluteFile();
            jGradleUserHomeEdit.setText(file.toString());
        }
    }//GEN-LAST:event_jBrowseUserHomeDirButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBrowseUserHomeDirButton;
    private javax.swing.JButton jChangeGradleLocationButton;
    private javax.swing.JTextField jGradleLocationDescription;
    private javax.swing.JLabel jGradlePathCaption;
    private javax.swing.JLabel jGradleUserHomeCaption;
    private javax.swing.JTextField jGradleUserHomeEdit;
    private javax.swing.JCheckBox jPreferWrapperCheck;
    // End of variables declaration//GEN-END:variables
}
