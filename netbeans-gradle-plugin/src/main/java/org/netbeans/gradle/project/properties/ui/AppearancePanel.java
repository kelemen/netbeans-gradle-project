package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
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
import org.netbeans.gradle.project.java.JavaExtensionDef;
import org.netbeans.gradle.project.java.properties.JavaProjectProperties;
import org.netbeans.gradle.project.properties.ExtensionActiveSettingsQuery;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbGuiUtils;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;

@SuppressWarnings("serial")
public class AppearancePanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Appearance");

    private static final CustomizerCategoryId CATEGORY_ID = new CustomizerCategoryId(
            AppearancePanel.class.getName() + ".settings",
            NbStrings.getAppearanceCategoryName());

    private boolean allowInherit;
    private String defaultPatternValue;

    public AppearancePanel(boolean allowInherit) {
        this.allowInherit = allowInherit;
        this.defaultPatternValue = "";

        initComponents();

        setupInitialInheritChecks(allowInherit, jSourcesDisplayModeInheritCheck, jProjectNodeNameInheritCheck);

        fillJavaSourcesDisplayModeCombo();
        fillPatternCombo();

        setupEnableDisable();
    }

    private static void setupInheritCheck(JCheckBox inheritCheck, JComponent... components) {
        NbGuiUtils.enableBasedOnCheck(inheritCheck, false, components);
    }

    private void setupEnableDisable() {
        setupInheritCheck(jSourcesDisplayModeInheritCheck, jSourcesDisplayMode);
        setupInheritCheck(jProjectNodeNameInheritCheck, jDisplayNameCombo, jCustomDisplayNameEdit);

        jDisplayNameCombo.addItemListener(e -> updateCustomEditVisibility());
        updateCustomEditVisibility();
    }

    public static ProfileBasedSettingsCategory createSettingsCategory(final boolean allowInherit) {
        return new ProfileBasedSettingsCategory(CATEGORY_ID, () -> AppearancePanel.createSettingsPage(allowInherit));
    }

    public static GlobalSettingsPage createSettingsPage(boolean allowInherit) {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new AppearancePanel(allowInherit));
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    private static void setupInitialInheritChecks(boolean allowInherit, JCheckBox... checkboxes) {
        if (!allowInherit) {
            for (JCheckBox checkbox: checkboxes) {
                checkbox.setSelected(false);
            }
        }
        for (JCheckBox checkbox: checkboxes) {
            checkbox.setVisible(allowInherit);
        }
    }

    private void updateCustomEditVisibility() {
        NamePatternItem selected = (NamePatternItem)jDisplayNameCombo.getSelectedItem();
        if (selected == null) {
            jCustomDisplayNameEdit.setVisible(false);
            return;
        }

        boolean newVisible = selected.pattern == null;
        if (newVisible != jCustomDisplayNameEdit.isVisible()) {
            jCustomDisplayNameEdit.setVisible(newVisible);
            validate();
            repaint();
        }
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private void fillJavaSourcesDisplayModeCombo() {
        jSourcesDisplayMode.removeAllItems();

        JavaSourcesDisplayMode[] displayModes = JavaSourcesDisplayMode.values();
        SourcesDisplayModeItem[] items = new SourcesDisplayModeItem[displayModes.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = new SourcesDisplayModeItem(displayModes[i]);
        }

        Arrays.sort(items, (SourcesDisplayModeItem o1, SourcesDisplayModeItem o2) -> {
            JavaSourcesDisplayMode mode1 = o1.displayMode;
            JavaSourcesDisplayMode mode2 = o2.displayMode;
            if (mode1 == mode2) {
                return 0;
            }
            if (mode1 == JavaSourcesDisplayMode.DEFAULT_MODE) {
                return -1;
            }
            if (mode2 == JavaSourcesDisplayMode.DEFAULT_MODE) {
                return 1;
            }

            return StringUtils.STR_CMP.compare(o1.toString(), o2.toString());
        });

        for (SourcesDisplayModeItem item: items) {
            jSourcesDisplayMode.addItem(item);
        }
    }

    private void selectSourcesDisplayMode(
            JavaSourcesDisplayMode newMode,
            PropertyReference<JavaSourcesDisplayMode> modeRef) {
        JavaSourcesDisplayMode shownMode = newMode != null ? newMode : modeRef.getActiveValue();
        if (shownMode != null) {
            jSourcesDisplayMode.setSelectedItem(new SourcesDisplayModeItem(shownMode));
        }

        if (allowInherit) {
            jSourcesDisplayModeInheritCheck.setSelected(newMode == null);
        }
    }

    private JavaSourcesDisplayMode getJavaSourcesDisplayMode() {
        if (allowInherit && jSourcesDisplayModeInheritCheck.isSelected()) {
            return null;
        }

        SourcesDisplayModeItem selected = (SourcesDisplayModeItem)jSourcesDisplayMode.getSelectedItem();
        if (selected == null) {
            return JavaSourcesDisplayMode.DEFAULT_MODE;
        }
        return selected.displayMode;
    }

    private void updatePattern(
            String value,
            PropertyReference<? extends String> valueWitFallbacks) {
        String displayedValue = value != null
                ? value
                : (valueWitFallbacks != null ? valueWitFallbacks.getActiveValue() : null);

        defaultPatternValue = Objects.requireNonNull(displayedValue, "displayedValue");
        selectPattern(defaultPatternValue);

        if (allowInherit) {
            jProjectNodeNameInheritCheck.setSelected(value == null);
        }
    }

    private String getNamePattern() {
        if (allowInherit && jProjectNodeNameInheritCheck.isSelected()) {
            return null;
        }

        NamePatternItem selected = (NamePatternItem)jDisplayNameCombo.getSelectedItem();
        if (selected == null) {
            return defaultPatternValue;
        }

        String pattern = selected.pattern;
        pattern = pattern != null ? pattern : jCustomDisplayNameEdit.getText().trim();
        if (pattern.isEmpty()) {
            return defaultPatternValue;
        }

        return pattern;
    }

    private void fillPatternCombo() {
        jDisplayNameCombo.removeAllItems();

        jDisplayNameCombo.addItem(new NamePatternItem(DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant()));
        jDisplayNameCombo.addItem(new NamePatternItem(DisplayableTaskVariable.PROJECT_PATH.getScriptReplaceConstant()));
        jDisplayNameCombo.addItem(new NamePatternItem(DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant()
                        + "-"
                        + DisplayableTaskVariable.PROJECT_VERSION.getScriptReplaceConstant()));
        jDisplayNameCombo.addItem(new NamePatternItem(DisplayableTaskVariable.PROJECT_GROUP.getScriptReplaceConstant()
                        + "."
                        + DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant()));
        jDisplayNameCombo.addItem(new NamePatternItem(DisplayableTaskVariable.PROJECT_GROUP.getScriptReplaceConstant()
                        + "."
                        + DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant()
                        + "-"
                        + DisplayableTaskVariable.PROJECT_VERSION.getScriptReplaceConstant()));
        jDisplayNameCombo.addItem(new NamePatternItem(DisplayableTaskVariable.PARENT_NAME.getScriptReplaceConstant()
                        + "."
                        + DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant()));
        jDisplayNameCombo.addItem(new NamePatternItem(NbStrings.getCustomNamePatternLabel(), null));
    }

    private void selectPattern(String namePattern) {
        jCustomDisplayNameEdit.setText(namePattern);

        int itemCount = jDisplayNameCombo.getItemCount();

        for (int i = 0; i < itemCount; i++) {
            NamePatternItem patternItem = jDisplayNameCombo.getItemAt(i);
            if (Objects.equals(namePattern, patternItem.pattern)) {
                jDisplayNameCombo.setSelectedIndex(i);
                return;
            }
        }

        for (int i = itemCount - 1; i >= 0; i--) {
            NamePatternItem patternItem = jDisplayNameCombo.getItemAt(i);
            if (patternItem.pattern == null) {
                jDisplayNameCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static final class NamePatternItem {
        private final String displayName;
        public final String pattern;

        public NamePatternItem(String pattern) {
            this(pattern, pattern);
        }

        public NamePatternItem(String displayName, String pattern) {
            this.displayName = displayName;
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class SourcesDisplayModeItem {
        private final String displayName;
        private final JavaSourcesDisplayMode displayMode;

        public SourcesDisplayModeItem(JavaSourcesDisplayMode displayMode) {
            this.displayName = NbStrings.getJavaSourcesDisplayMode(displayMode);
            this.displayMode = displayMode;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.displayMode);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final SourcesDisplayModeItem other = (SourcesDisplayModeItem)obj;
            return this.displayMode == other.displayMode;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<String> displayNamePatternRef;
        private final PropertyReference<JavaSourcesDisplayMode> javaSourcesDisplayModeRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            displayNamePatternRef = NbGradleCommonProperties.displayNamePattern(settingsQuery);

            ActiveSettingsQuery javaExtQuery
                    = new ExtensionActiveSettingsQuery(settingsQuery, JavaExtensionDef.EXTENSION_NAME);
            javaSourcesDisplayModeRef = JavaProjectProperties.javaSourcesDisplayMode(javaExtQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, AppearancePanel.this);
        }
    }

    private class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final String displayNamePattern;
        private final JavaSourcesDisplayMode javaSourcesDisplayMode;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.displayNamePattern = properties.displayNamePatternRef.tryGetValueWithoutFallback();
            this.javaSourcesDisplayMode = properties.javaSourcesDisplayModeRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, AppearancePanel panel) {
            this.properties = properties;
            this.displayNamePattern = panel.getNamePattern();
            this.javaSourcesDisplayMode = panel.getJavaSourcesDisplayMode();
        }

        @Override
        public void displaySettings() {
            updatePattern(displayNamePattern, properties.displayNamePatternRef);
            selectSourcesDisplayMode(javaSourcesDisplayMode, properties.javaSourcesDisplayModeRef);
        }

        @Override
        public void saveSettings() {
            properties.displayNamePatternRef.setValue(displayNamePattern);
            properties.javaSourcesDisplayModeRef.setValue(javaSourcesDisplayMode);
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "Convert2Diamond"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jProjectNodeNameHolder = new javax.swing.JPanel();
        jSourcesDisplayCaption = new javax.swing.JLabel();
        jSourcesDisplayMode = new javax.swing.JComboBox<>();
        jCustomDisplayNameEdit = new javax.swing.JTextField();
        jProjectNodeNameInheritCheck = new javax.swing.JCheckBox();
        jDisplayNameCombo = new javax.swing.JComboBox<>();
        jDisplayNameCaption = new javax.swing.JLabel();
        jSourcesDisplayModeInheritCheck = new javax.swing.JCheckBox();

        jProjectNodeNameHolder.setLayout(new java.awt.GridLayout(1, 1));

        org.openide.awt.Mnemonics.setLocalizedText(jSourcesDisplayCaption, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jSourcesDisplayCaption.text")); // NOI18N

        jCustomDisplayNameEdit.setText(org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jCustomDisplayNameEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jProjectNodeNameInheritCheck, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jProjectNodeNameInheritCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDisplayNameCaption, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jDisplayNameCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourcesDisplayModeInheritCheck, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jSourcesDisplayModeInheritCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jProjectNodeNameHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jDisplayNameCaption)
                    .addComponent(jSourcesDisplayCaption))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jDisplayNameCombo, 0, 184, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jProjectNodeNameInheritCheck))
                    .addComponent(jCustomDisplayNameEdit)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jSourcesDisplayMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSourcesDisplayModeInheritCheck)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jProjectNodeNameHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDisplayNameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jDisplayNameCaption)
                    .addComponent(jProjectNodeNameInheritCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCustomDisplayNameEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourcesDisplayMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSourcesDisplayCaption)
                    .addComponent(jSourcesDisplayModeInheritCheck))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jCustomDisplayNameEdit;
    private javax.swing.JLabel jDisplayNameCaption;
    private javax.swing.JComboBox<NamePatternItem> jDisplayNameCombo;
    private javax.swing.JPanel jProjectNodeNameHolder;
    private javax.swing.JCheckBox jProjectNodeNameInheritCheck;
    private javax.swing.JLabel jSourcesDisplayCaption;
    private javax.swing.JComboBox<SourcesDisplayModeItem> jSourcesDisplayMode;
    private javax.swing.JCheckBox jSourcesDisplayModeInheritCheck;
    // End of variables declaration//GEN-END:variables
}
