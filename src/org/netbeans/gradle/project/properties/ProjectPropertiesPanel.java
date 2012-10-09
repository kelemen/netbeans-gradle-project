package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.NbStrings;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.modules.SpecificationVersion;

@SuppressWarnings("serial") // don't care
public class ProjectPropertiesPanel extends javax.swing.JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProjectPropertiesPanel.class.getName());
    private ProjectProperties currentProperties;

    public ProjectPropertiesPanel() {
        initComponents();

        currentProperties = null;
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        List<PlatformComboItem> comboItems = new LinkedList<PlatformComboItem>();
        for (int i = 0; i < platforms.length; i++) {
            JavaPlatform platform = platforms[i];
            Specification specification = platform.getSpecification();
            if (specification != null && specification.getVersion() != null) {
                comboItems.add(new PlatformComboItem(platform));
            }
        }

        jPlatformCombo.setModel(new DefaultComboBoxModel(comboItems.toArray(new PlatformComboItem[0])));
    }

    public void initFromProperties(ProjectProperties properties) {
        currentProperties = properties;

        JavaPlatform currentPlatform = properties.getPlatform().getValue();
        jPlatformCombo.setSelectedItem(new PlatformComboItem(currentPlatform));

        jSourceEncoding.setText(properties.getSourceEncoding().getValue().name());

        jSourceLevelCombo.setSelectedItem(properties.getSourceLevel().getValue());
    }

    public void updateProperties(ProjectProperties properties) {
        PlatformComboItem selected = (PlatformComboItem)jPlatformCombo.getSelectedItem();
        if (selected != null) {
            properties.getPlatform().setValue(selected.getPlatform());
        }

        String charsetName = jSourceEncoding.getText().trim();
        try {
            Charset newEncoding = Charset.forName(charsetName);
            properties.getSourceEncoding().setValue(newEncoding);
        } catch (IllegalCharsetNameException ex) {
            LOGGER.log(Level.INFO, "Illegal character set: " + charsetName, ex);
        } catch (UnsupportedCharsetException ex) {
            LOGGER.log(Level.INFO, "Unsupported character set: " + charsetName, ex);
        }

        String sourceLevel = (String)jSourceLevelCombo.getSelectedItem();
        if (sourceLevel != null) {
            properties.getSourceLevel().setValue(sourceLevel);
        }
    }

    public JavaPlatform getSelectedPlatform() {
        PlatformComboItem selected = (PlatformComboItem)jPlatformCombo.getSelectedItem();
        return selected != null ? selected.getPlatform() : JavaPlatform.getDefault();
    }

    public Charset getSourceEncoding() {
        try {
            return Charset.forName(jSourceEncoding.getText().trim());
        } catch (IllegalCharsetNameException ex) {
            return MemProjectProperties.DEFAULT_SOURCE_ENCODING;
        } catch (UnsupportedCharsetException ex) {
            return MemProjectProperties.DEFAULT_SOURCE_ENCODING;
        }
    }

    private static class PlatformComboItem {
        private final JavaPlatform platform;

        public PlatformComboItem(JavaPlatform platform) {
            if (platform == null) throw new NullPointerException("platform");
            this.platform = platform;
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + (this.platform.getSpecification().getVersion().hashCode());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PlatformComboItem other = (PlatformComboItem)obj;
            SpecificationVersion thisVersion = this.platform.getSpecification().getVersion();
            SpecificationVersion otherVersion = other.platform.getSpecification().getVersion();
            return thisVersion.equals(otherVersion);
        }

        @Override
        public String toString() {
            return platform.getDisplayName();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSourceEncodingCaption = new javax.swing.JLabel();
        jSourceEncoding = new javax.swing.JTextField();
        jPlatformCaption = new javax.swing.JLabel();
        jPlatformCombo = new javax.swing.JComboBox();
        jSourceLevelCaption = new javax.swing.JLabel();
        jSourceLevelCombo = new javax.swing.JComboBox();
        jManageTasksButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceEncodingCaption.text")); // NOI18N

        jSourceEncoding.setText(org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceEncoding.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jPlatformCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceLevelCaption.text")); // NOI18N

        jSourceLevelCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.3", "1.4", "1.5", "1.6", "1.7", "1.8" }));

        org.openide.awt.Mnemonics.setLocalizedText(jManageTasksButton, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jManageTasksButton.text")); // NOI18N
        jManageTasksButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jManageTasksButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSourceEncoding)
                    .addComponent(jPlatformCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceEncodingCaption)
                            .addComponent(jPlatformCaption)
                            .addComponent(jSourceLevelCaption))
                        .addGap(0, 303, Short.MAX_VALUE))
                    .addComponent(jSourceLevelCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jManageTasksButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSourceEncodingCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSourceEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPlatformCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPlatformCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSourceLevelCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jManageTasksButton))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jManageTasksButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jManageTasksButtonActionPerformed
        if (currentProperties == null) {
            LOGGER.warning("Project properties were not set.");
            return;
        }

        ManageTasksPanel panel = new ManageTasksPanel();
        panel.initSettings(currentProperties);

        DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getManageTasksDlgTitle(),
                true,
                new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);
        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.pack();
        dlg.setVisible(true);

        if (DialogDescriptor.OK_OPTION == dlgDescriptor.getValue()) {
            panel.saveTasks(currentProperties);
        }
    }//GEN-LAST:event_jManageTasksButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jManageTasksButton;
    private javax.swing.JLabel jPlatformCaption;
    private javax.swing.JComboBox jPlatformCombo;
    private javax.swing.JTextField jSourceEncoding;
    private javax.swing.JLabel jSourceEncodingCaption;
    private javax.swing.JLabel jSourceLevelCaption;
    private javax.swing.JComboBox jSourceLevelCombo;
    // End of variables declaration//GEN-END:variables
}
