package org.netbeans.gradle.project.properties;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.util.NbGuiUtils;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;

@SuppressWarnings("serial")
public class ProjectNodeNamePanel extends javax.swing.JPanel {
    private String defaultValue;

    public ProjectNodeNamePanel(boolean allowInherit) {
        defaultValue = "";

        initComponents();

        if (!allowInherit) {
            jInheritCheck.setSelected(false);
        }
        jInheritCheck.setVisible(allowInherit);

        fillPatternCombo();

        jDisplayNameCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateCustomEditVisibility();
            }
        });
        updateCustomEditVisibility();

        setupEnableDisable();
    }

    private static void setupInheritCheck(JCheckBox inheritCheck, JComponent... components) {
        NbGuiUtils.enableBasedOnCheck(inheritCheck, false, components);
    }

    private void setupEnableDisable() {
        setupInheritCheck(jInheritCheck, jDisplayNameCombo, jCustomDisplayNameEdit);
    }

    public void updatePattern(
            String value,
            PropertyReference<? extends String> valueWitFallbacks) {
        String displayedValue = value != null
                ? value
                : (valueWitFallbacks != null ? valueWitFallbacks.getActiveValue() : null);
        ExceptionHelper.checkNotNullArgument(displayedValue, "displayedValue");

        defaultValue = displayedValue;
        selectPattern(defaultValue);

        jInheritCheck.setSelected(value == null);
    }

    public String getNamePattern() {
        if (isInherit()) {
            return null;
        }

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

    public boolean isInherit() {
        return jInheritCheck.isSelected();
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

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCustomDisplayNameEdit = new javax.swing.JTextField();
        jDisplayNameCaption = new javax.swing.JLabel();
        jDisplayNameCombo = new javax.swing.JComboBox<>();
        jInheritCheck = new javax.swing.JCheckBox();

        jCustomDisplayNameEdit.setText(org.openide.util.NbBundle.getMessage(ProjectNodeNamePanel.class, "ProjectNodeNamePanel.jCustomDisplayNameEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jDisplayNameCaption, org.openide.util.NbBundle.getMessage(ProjectNodeNamePanel.class, "ProjectNodeNamePanel.jDisplayNameCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jInheritCheck, org.openide.util.NbBundle.getMessage(ProjectNodeNamePanel.class, "ProjectNodeNamePanel.jInheritCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDisplayNameCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCustomDisplayNameEdit, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jDisplayNameCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jInheritCheck)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDisplayNameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jDisplayNameCaption)
                    .addComponent(jInheritCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCustomDisplayNameEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jCustomDisplayNameEdit;
    private javax.swing.JLabel jDisplayNameCaption;
    private javax.swing.JComboBox<NamePatternItem> jDisplayNameCombo;
    private javax.swing.JCheckBox jInheritCheck;
    // End of variables declaration//GEN-END:variables
}
