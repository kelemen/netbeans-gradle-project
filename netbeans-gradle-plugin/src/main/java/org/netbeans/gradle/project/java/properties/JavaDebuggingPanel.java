package org.netbeans.gradle.project.java.properties;

import java.net.URL;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.ui.EnumCombo;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbGuiUtils;

@SuppressWarnings("serial")
public class JavaDebuggingPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Debug-Settings");

    private final boolean allowInherit;
    private final EnumCombo<DebugMode> debugModeComboHandler;

    public JavaDebuggingPanel(boolean allowInherit) {
        this.allowInherit = allowInherit;

        initComponents();
        debugModeComboHandler = new EnumCombo<>(DebugMode.class, DebugMode.DEBUGGER_ATTACHES, jDebugMode);
        if (!allowInherit) {
            jDebugModeInherit.setVisible(false);
            jDebugModeInherit.setSelected(false);
        }

        setupEnableDisable();
    }

    private void setupEnableDisable() {
        setupInheritCheck(jDebugModeInherit, jDebugMode);
    }

    private static void setupInheritCheck(JCheckBox inheritCheck, JComponent... components) {
        NbGuiUtils.enableBasedOnCheck(inheritCheck, false, components);
    }

    private <Value> Value setInheritAndGetValue(
            Value value,
            PropertyReference<? extends Value> valueWithFallbacks,
            JCheckBox inheritCheck) {
        inheritCheck.setSelected(allowInherit && value == null);
        return value != null ? value : valueWithFallbacks.getActiveValue();
    }

    private static CustomizerCategoryId getCategoryId() {
        return new CustomizerCategoryId(JavaDebuggingPanel.class.getName(), NbStrings.getSettingsCategoryDebugJava());
    }

    public static ProfileBasedSettingsCategory createDebuggingCustomizer(final boolean allowInherit) {
        return new ProfileBasedSettingsCategory(getCategoryId(), () -> JavaDebuggingPanel.createSettingsPage(allowInherit));
    }

    public static GlobalSettingsPage createSettingsPage(boolean allowInherit) {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new JavaDebuggingPanel(allowInherit));
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<DebugMode> debugModeRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            this.debugModeRef = JavaProjectProperties.debugMode(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, JavaDebuggingPanel.this);
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;
        private final DebugMode debugMode;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.debugMode = properties.debugModeRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, JavaDebuggingPanel panel) {
            this.properties = properties;

            this.debugMode = allowInherit && panel.jDebugModeInherit.isSelected()
                    ? null
                    : panel.debugModeComboHandler.getSelectedValue();
        }

        @Override
        public void displaySettings() {
            DebugMode activeDebugMode = setInheritAndGetValue(debugMode, properties.debugModeRef, jDebugModeInherit);
            debugModeComboHandler.setSelectedValue(activeDebugMode);
        }

        @Override
        public void saveSettings() {
            properties.debugModeRef.setValue(debugMode);
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

        jDebugMode = new javax.swing.JComboBox<>();
        jDebugModeCaption = new javax.swing.JLabel();
        jDebugModeInherit = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jDebugModeCaption, org.openide.util.NbBundle.getMessage(JavaDebuggingPanel.class, "JavaDebuggingPanel.jDebugModeCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDebugModeInherit, org.openide.util.NbBundle.getMessage(JavaDebuggingPanel.class, "JavaDebuggingPanel.jDebugModeInherit.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDebugModeCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jDebugMode, 0, 185, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jDebugModeInherit)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDebugModeCaption)
                    .addComponent(jDebugMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jDebugModeInherit))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<EnumCombo.Item<DebugMode>> jDebugMode;
    private javax.swing.JLabel jDebugModeCaption;
    private javax.swing.JCheckBox jDebugModeInherit;
    // End of variables declaration//GEN-END:variables
}
