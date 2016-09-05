package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.java.JavaExtensionDef;
import org.netbeans.gradle.project.java.properties.JavaProjectProperties;
import org.netbeans.gradle.project.properties.ExtensionActiveSettingsQuery;
import org.netbeans.gradle.project.properties.global.DebugMode;
import org.netbeans.gradle.project.properties.global.GlobalSettingsEditor;
import org.netbeans.gradle.project.properties.global.SettingsEditorProperties;
import org.netbeans.gradle.project.util.NbFileUtils;

@SuppressWarnings("serial")
public class DebuggerPanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Debug-Settings");

    private final EnumCombo<DebugMode> debugModeHandler;

    public DebuggerPanel() {
        initComponents();
        this.debugModeHandler = new EnumCombo<>(DebugMode.class, DebugMode.DEBUGGER_ATTACHES, jDebugMode);
    }

    private static ActiveSettingsQuery javaSettings(ActiveSettingsQuery rootSettings) {
        // FIXME: Once we allow extensions to define their own global settings page,
        //        this explicit conversion won't be be needed anymore.
        return new ExtensionActiveSettingsQuery(rootSettings, JavaExtensionDef.EXTENSION_NAME);
    }

    @Override
    public void updateSettings(ActiveSettingsQuery globalSettings) {
        ActiveSettingsQuery javaSettings = javaSettings(globalSettings);

        PropertyReference<DebugMode> debugMode = JavaProjectProperties.debugMode(javaSettings);
        debugModeHandler.setSelectedValue(debugMode.getActiveValue());
    }

    @Override
    public void saveSettings(ActiveSettingsQuery globalSettings) {
        ActiveSettingsQuery javaSettings = javaSettings(globalSettings);

        PropertyReference<DebugMode> debugMode = JavaProjectProperties.debugMode(javaSettings);
        debugMode.setValue(debugModeHandler.getSelectedValue());
    }

    @Override
    public SettingsEditorProperties getProperties() {
        SettingsEditorProperties.Builder result = new SettingsEditorProperties.Builder(this);
        result.setHelpUrl(HELP_URL);

        return result.create();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDebugMode = new javax.swing.JComboBox<>();
        jDebugModeCaption = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jDebugModeCaption, org.openide.util.NbBundle.getMessage(DebuggerPanel.class, "DebuggerPanel.jDebugModeCaption.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDebugModeCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jDebugMode, 0, 309, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDebugModeCaption)
                    .addComponent(jDebugMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<EnumCombo.Item<DebugMode>> jDebugMode;
    private javax.swing.JLabel jDebugModeCaption;
    // End of variables declaration//GEN-END:variables
}
