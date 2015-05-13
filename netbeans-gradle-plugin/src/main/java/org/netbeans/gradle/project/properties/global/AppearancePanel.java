package org.netbeans.gradle.project.properties.global;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.Objects;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;

@SuppressWarnings("serial")
public class AppearancePanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Appearance");

    private String defaultValue;

    public AppearancePanel() {
        defaultValue = "";

        initComponents();

        fillPatternCombo();
        fillJavaSourcesDisplayModeCombo();

        jDisplayNameCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateCustomEditVisibility();
            }
        });
        updateCustomEditVisibility();
    }

    private void updateCustomEditVisibility() {
        NamePatternItem selected = (NamePatternItem)jDisplayNameCombo.getSelectedItem();
        if (selected == null) {
            jCustomDisplayNameEdit.setVisible(false);
            return;
        }

        jCustomDisplayNameEdit.setVisible(selected.pattern == null);
    }

    private void fillJavaSourcesDisplayModeCombo() {
        jSourcesDisplayMode.removeAllItems();

        // TODO: I18N
        jSourcesDisplayMode.addItem(new SourcesDisplayModeItem("Default", JavaSourcesDisplayMode.DEFAULT_MODE));
        jSourcesDisplayMode.addItem(new SourcesDisplayModeItem("Group by sourceset", JavaSourcesDisplayMode.GROUP_BY_SOURCESET));
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
        jDisplayNameCombo.addItem(new NamePatternItem(NbStrings.getCustomNamePatternLabel(), null));
    }

    @Override
    public void updateSettings(GlobalGradleSettings globalSettings) {
        String namePattern = globalSettings.displayNamePattern().getValue();
        defaultValue = namePattern;
        selectPattern(namePattern);

        JavaSourcesDisplayMode sourcesDisplayMode = globalSettings.javaSourcesDisplayMode().getValue();
        selectSourcesDisplayMode(sourcesDisplayMode);
    }

    private void selectSourcesDisplayMode(JavaSourcesDisplayMode newMode) {
        jSourcesDisplayMode.setSelectedItem(new SourcesDisplayModeItem("", newMode));
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

    @Override
    public void saveSettings(GlobalGradleSettings globalSettings) {
        globalSettings.displayNamePattern().setValue(getNamePattern());
        globalSettings.javaSourcesDisplayMode().setValue(javaSourcesDisplayMode());

    }

    private JavaSourcesDisplayMode javaSourcesDisplayMode() {
        SourcesDisplayModeItem selected = (SourcesDisplayModeItem)jSourcesDisplayMode.getSelectedItem();
        if (selected == null) {
            return JavaSourcesDisplayMode.DEFAULT_MODE;
        }
        return selected.displayMode;
    }

    private String getNamePattern() {
        NamePatternItem selected = (NamePatternItem)jDisplayNameCombo.getSelectedItem();
        if (selected == null) {
            return defaultValue;
        }

        String pattern = selected.pattern;
        pattern = pattern != null ? pattern : jCustomDisplayNameEdit.getText().trim();
        if (pattern.isEmpty()) {
            return defaultValue;
        }

        return pattern;
    }

    @Override
    public SettingsEditorProperties getProperties() {
        SettingsEditorProperties.Builder result = new SettingsEditorProperties.Builder(this);
        result.setHelpUrl(HELP_URL);

        return result.create();
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

        public SourcesDisplayModeItem(String displayName, JavaSourcesDisplayMode displayMode) {
            this.displayName = displayName;
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

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "Convert2Diamond"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDisplayNameCaption = new javax.swing.JLabel();
        jDisplayNameCombo = new javax.swing.JComboBox<NamePatternItem>();
        jCustomDisplayNameEdit = new javax.swing.JTextField();
        jSourcesDisplayCaption = new javax.swing.JLabel();
        jSourcesDisplayMode = new javax.swing.JComboBox<SourcesDisplayModeItem>();

        org.openide.awt.Mnemonics.setLocalizedText(jDisplayNameCaption, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jDisplayNameCaption.text")); // NOI18N

        jCustomDisplayNameEdit.setText(org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jCustomDisplayNameEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourcesDisplayCaption, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jSourcesDisplayCaption.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jDisplayNameCaption)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCustomDisplayNameEdit)
                            .addComponent(jDisplayNameCombo, 0, 275, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jSourcesDisplayCaption)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSourcesDisplayMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDisplayNameCaption)
                    .addComponent(jDisplayNameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCustomDisplayNameEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourcesDisplayCaption)
                    .addComponent(jSourcesDisplayMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jCustomDisplayNameEdit;
    private javax.swing.JLabel jDisplayNameCaption;
    private javax.swing.JComboBox<NamePatternItem> jDisplayNameCombo;
    private javax.swing.JLabel jSourcesDisplayCaption;
    private javax.swing.JComboBox<SourcesDisplayModeItem> jSourcesDisplayMode;
    // End of variables declaration//GEN-END:variables
}
