package org.netbeans.gradle.project.java.properties;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedConfigurations;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPageFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.ui.EnumCombo;
import org.netbeans.gradle.project.util.NbGuiUtils;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;

@SuppressWarnings("serial")
public class JavaDebuggingPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private final EnumCombo<DebugMode> debugModeComboHandler;

    public JavaDebuggingPanel() {
        initComponents();
        debugModeComboHandler = new EnumCombo<>(DebugMode.class, DebugMode.DEBUGGER_ATTACHES, jDebugMode);

        setupEnableDisable();
    }

    private void setupEnableDisable() {
        setupInheritCheck(jDebugModeInherit, jDebugMode);
    }

    private static void setupInheritCheck(JCheckBox inheritCheck, JComponent... components) {
        NbGuiUtils.enableBasedOnCheck(inheritCheck, false, components);
    }

    private static <Value> Value setInheritAndGetValue(
            Value value,
            PropertyReference<? extends Value> valueWithFallbacks,
            JCheckBox inheritCheck) {
        inheritCheck.setSelected(value == null);
        return value != null ? value : valueWithFallbacks.getActiveValue();
    }

    public static ProjectCustomizer.CompositeCategoryProvider createDebuggingCustomizer(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        Project project = javaExt.getProject();
        // TODO: I18N
        CustomizerCategoryId categoryId = new CustomizerCategoryId(JavaDebuggingPanel.class.getName(), "Debugging - Java");
        ProjectSettingsProvider.ExtensionSettings extensionSettings = javaExt.getExtensionSettings();

        return ProfileBasedConfigurations.createProfileBasedCustomizer(project, categoryId, extensionSettings, new ProfileBasedSettingsPageFactory() {
            @Override
            public ProfileBasedSettingsPage createSettingsPage() {
                JavaDebuggingPanel customPanel = new JavaDebuggingPanel();
                return new ProfileBasedSettingsPage(customPanel, customPanel);
            }
        });
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

            this.debugMode = panel.jDebugModeInherit.isSelected()
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
