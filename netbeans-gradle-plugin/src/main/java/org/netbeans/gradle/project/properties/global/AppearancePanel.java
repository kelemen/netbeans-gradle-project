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
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Global-Settings");

    private String defaultValue;

    public AppearancePanel() {
        defaultValue = "";

        initComponents();

        fillPatternCombo();

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
            if (patternItem == null) {
                jDisplayNameCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public void saveSettings(GlobalGradleSettings globalSettings) {
        globalSettings.displayNamePattern().setValue(getNamePattern());

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

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDisplayNameCaption = new javax.swing.JLabel();
        jDisplayNameCombo = new javax.swing.JComboBox<NamePatternItem>();
        jCustomDisplayNameEdit = new javax.swing.JTextField();

        org.openide.awt.Mnemonics.setLocalizedText(jDisplayNameCaption, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jDisplayNameCaption.text")); // NOI18N

        jCustomDisplayNameEdit.setText(org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jCustomDisplayNameEdit.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDisplayNameCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCustomDisplayNameEdit)
                    .addComponent(jDisplayNameCombo, 0, 275, Short.MAX_VALUE))
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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jCustomDisplayNameEdit;
    private javax.swing.JLabel jDisplayNameCaption;
    private javax.swing.JComboBox<NamePatternItem> jDisplayNameCombo;
    // End of variables declaration//GEN-END:variables
}
