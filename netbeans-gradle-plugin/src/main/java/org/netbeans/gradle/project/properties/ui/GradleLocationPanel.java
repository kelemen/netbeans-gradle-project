package org.netbeans.gradle.project.properties.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import org.gradle.util.GradleVersion;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.properties.GradleLocationDirectory;
import org.netbeans.gradle.project.properties.GradleLocationDistribution;
import org.netbeans.gradle.project.properties.GradleLocationRef;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.netbeans.gradle.project.util.NbGuiUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileChooserBuilder;


@SuppressWarnings("serial")
public class GradleLocationPanel extends javax.swing.JPanel {
    private final ChangeListenerManager locationChangedListeners;
    private final StringResolver locationResolver;

    public GradleLocationPanel(StringResolver locationResolver, GradleLocationRef defaultLocationRef) {
        this.locationResolver = Objects.requireNonNull(locationResolver, "locationResolver");

        locationChangedListeners = new GenericChangeListenerManager();

        initComponents();

        jFolderEdit.setText("");

        GradleVersion version = GradleVersion.current();

        jUriEdit.setText(getUriStrForVersion(version));
        jVersionEdit.setText(version.getVersion());

        ButtonGroup group = new ButtonGroup();
        group.add(jDefaultCheck);
        group.add(jDistCheck);
        group.add(jLocalDirCheck);
        group.add(jVersionCheck);

        jDefaultCheck.setSelected(true);

        selectLocation(defaultLocationRef != null
                ? defaultLocationRef
                : GradleLocationDefault.DEFAULT_REF);

        NbGuiUtils.enableBasedOnCheck(jDistCheck, true, jUriEdit);
        NbGuiUtils.enableBasedOnCheck(jLocalDirCheck, true, jFolderEdit, jFolderSelectButton);
        NbGuiUtils.enableBasedOnCheck(jVersionCheck, true, jVersionEdit);

        setupLocationChangeListeners();
    }

    public static GradleLocationRef tryChooseLocation(
            Component parent,
            StringResolver locationResolver,
            GradleLocationRef defaultLocationRef) {
        GradleLocationPanel panel = new GradleLocationPanel(locationResolver, defaultLocationRef);

        final DialogDescriptor dlgDescriptor = new DialogDescriptor(
            panel,
            NbStrings.getGradleLocationDlgTitle(),
            true,
            new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
            DialogDescriptor.OK_OPTION,
            DialogDescriptor.BOTTOM_ALIGN,
            null,
            null);

        final PropertySource<Boolean> validLocation = panel.validLocation();
        dlgDescriptor.setValid(validLocation.getValue());

        validLocation.addChangeListener(() -> {
            dlgDescriptor.setValid(validLocation.getValue());
        });

        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.setLocationRelativeTo(parent);
        dlg.pack();
        dlg.setVisible(true);

        if (DialogDescriptor.OK_OPTION != dlgDescriptor.getValue()) {
            return null;
        }

        return panel.getSelectedLocation();
    }

    private void setupLocationChangeListeners() {
        Runnable fireEventTask = this::fireLocationChangeEvent;

        SwingProperties.buttonSelected(jDefaultCheck).addChangeListener(fireEventTask);
        SwingProperties.buttonSelected(jDistCheck).addChangeListener(fireEventTask);
        SwingProperties.buttonSelected(jLocalDirCheck).addChangeListener(fireEventTask);
        SwingProperties.buttonSelected(jVersionCheck).addChangeListener(fireEventTask);
        SwingProperties.textProperty(jUriEdit).addChangeListener(fireEventTask);
        SwingProperties.textProperty(jFolderEdit).addChangeListener(fireEventTask);
        SwingProperties.textProperty(jVersionEdit).addChangeListener(fireEventTask);
    }

    private void fireLocationChangeEvent() {
        locationChangedListeners.fireEventually();
    }

    private static String getUriStrForVersion(GradleVersion version) {
        String versionStr = version.getVersion();
        if (version.isSnapshot()) {
            return "http://services.gradle.org/distributions-snapshots/gradle-" + versionStr + "-bin.zip";
        }
        else {
            return "http://services.gradle.org/distributions/gradle-" + versionStr + "-bin.zip";
        }
    }

    private void selectLocation(GradleLocationRef locationRef) {
        final AtomicReference<JTextField> locationEditRef = new AtomicReference<>();
        locationRef.getLocation(locationResolver).applyLocation(new GradleLocation.Applier() {
            @Override
            public void applyVersion(String versionStr) {
                jVersionCheck.setSelected(true);
                locationEditRef.set(jVersionEdit);
            }

            @Override
            public void applyDirectory(File gradleHome) {
                jLocalDirCheck.setSelected(true);
                locationEditRef.set(jFolderEdit);
            }

            @Override
            public void applyDistribution(URI uri) {
                jDistCheck.setSelected(true);
                locationEditRef.set(jUriEdit);
            }

            @Override
            public void applyDefault() {
                jDefaultCheck.setSelected(true);
            }
        });

        JTextField locationEdit = locationEditRef.get();
        if (locationEdit != null) {
            String locationStr = locationRef.asString();
            if (locationStr == null) {
                locationStr = "";
            }
            locationEdit.setText(locationStr);
        }
    }

    public PropertySource<Boolean> validLocation() {
        return new PropertySource<Boolean>() {
            @Override
            public Boolean getValue() {
                return hasValidLocationSelected();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return locationChangedListeners.registerListener(listener);
            }
        };
    }

    private static boolean isAllVarsValid(String str) {
        return replaceIfAllVarsValid(str) != null;
    }

    private static String replaceIfAllVarsValid(String str) {
        return StringResolvers.getDefaultGlobalResolver().resolveStringIfValid(str);
    }

    private URI tryParseUri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private boolean hasValidLocationSelected() {
        if (jVersionCheck.isSelected()) {
            String versionStr = jVersionEdit.getText().trim();
            return isAllVarsValid(versionStr);
        }
        if (jDistCheck.isSelected()) {
            String uriStr = jUriEdit.getText().trim();
            String resolvedUri = replaceIfAllVarsValid(uriStr);
            if (resolvedUri == null) {
                return false;
            }
            return tryParseUri(resolvedUri) != null;
        }
        if (jLocalDirCheck.isSelected()) {
            String dirStr = jFolderEdit.getText().trim();
            return isAllVarsValid(dirStr);
        }
        return true;
    }

    public GradleLocationRef getSelectedLocation() {
        if (jVersionCheck.isSelected()) {
            return GradleLocationVersion.getLocationRef(jVersionEdit.getText().trim());
        }
        if (jDistCheck.isSelected()) {
            return GradleLocationDistribution.getLocationRef(jUriEdit.getText().trim());
        }
        if (jLocalDirCheck.isSelected()) {
            return GradleLocationDirectory.getLocationRef(jFolderEdit.getText().trim());
        }

        return GradleLocationDefault.DEFAULT_REF;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jVersionCheck = new javax.swing.JRadioButton();
        jDefaultCheck = new javax.swing.JRadioButton();
        jDistCheck = new javax.swing.JRadioButton();
        jLocalDirCheck = new javax.swing.JRadioButton();
        jVersionEdit = new javax.swing.JTextField();
        jUriEdit = new javax.swing.JTextField();
        jFolderEdit = new javax.swing.JTextField();
        jFolderSelectButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jVersionCheck, org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jVersionCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDefaultCheck, org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jDefaultCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDistCheck, org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jDistCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLocalDirCheck, org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jLocalDirCheck.text")); // NOI18N

        jVersionEdit.setText(org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jVersionEdit.text")); // NOI18N

        jUriEdit.setText(org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jUriEdit.text")); // NOI18N

        jFolderEdit.setText(org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jFolderEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jFolderSelectButton, org.openide.util.NbBundle.getMessage(GradleLocationPanel.class, "GradleLocationPanel.jFolderSelectButton.text")); // NOI18N
        jFolderSelectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFolderSelectButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jVersionCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jVersionEdit, javax.swing.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jDistCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jUriEdit))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jDefaultCheck)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLocalDirCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jFolderEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jFolderSelectButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDefaultCheck)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jVersionCheck)
                    .addComponent(jVersionEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDistCheck)
                    .addComponent(jUriEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLocalDirCheck)
                    .addComponent(jFolderEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jFolderSelectButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jFolderSelectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFolderSelectButtonActionPerformed
        FileChooserBuilder dlgChooser = new FileChooserBuilder(GradleLocationPanel.class);
        dlgChooser.setDirectoriesOnly(true);
        File f = dlgChooser.showOpenDialog();
        if (f != null && f.isDirectory()) {
            File file = f.getAbsoluteFile();
            jFolderEdit.setText(file.toString());
        }
    }//GEN-LAST:event_jFolderSelectButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton jDefaultCheck;
    private javax.swing.JRadioButton jDistCheck;
    private javax.swing.JTextField jFolderEdit;
    private javax.swing.JButton jFolderSelectButton;
    private javax.swing.JRadioButton jLocalDirCheck;
    private javax.swing.JTextField jUriEdit;
    private javax.swing.JRadioButton jVersionCheck;
    private javax.swing.JTextField jVersionEdit;
    // End of variables declaration//GEN-END:variables
}
