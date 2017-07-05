package org.netbeans.gradle.project.properties.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.standard.CustomVariable;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.properties.standard.MemCustomVariables;
import org.netbeans.gradle.project.util.StringUtils;

@SuppressWarnings("serial")
public class CustomVariablesPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final CustomizerCategoryId CATEGORY_ID = new CustomizerCategoryId(
            CustomVariablesPanel.class.getName() + ".settings",
            NbStrings.getCustomVariablesCategoryName());

    private CustomVariablesPanel() {
        initComponents();
    }

    public static ProfileBasedSettingsCategory createSettingsCategory() {
        return new ProfileBasedSettingsCategory(CATEGORY_ID, CustomVariablesPanel::createSettingsPage);
    }

    public static ProfileBasedSettingsPage createSettingsPage() {
        CustomVariablesPanel result = new CustomVariablesPanel();
        return new ProfileBasedSettingsPage(result, result);
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private static boolean containsMustBeEscapedChar(String keyName) {
        for (int i = 0; i < keyName.length(); i++) {
            char ch = keyName.charAt(i);
            if (ch <= ' ' || ch == '=') {
                return true;
            }
        }
        return false;
    }

    private static String escapeKeyName(String keyName) {
        if (!containsMustBeEscapedChar(keyName)) {
            return keyName;
        }

        StringBuilder result = new StringBuilder(keyName.length() * 2);
        for (int i = 0; i < keyName.length(); i++) {
            char ch = keyName.charAt(i);
            if (ch == '=' || ch == ' ') {
                result.append('\\');
                result.append(ch);
            }
            else if (ch < ' ') {
                result.append("\\u");
                StringUtils.appendHexFixedLength(ch, result);
            }
            else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static String toProperties(CustomVariables vars) {
        if (vars == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(1024);
        for (CustomVariable var: vars.getVariables()) {
            result.append(escapeKeyName(var.getName()));
            result.append('=');
            result.append(var.getValue());
            result.append('\n');
        }
        return result.toString();
    }

    private static int parseKey(String line, StringBuilder key) {
        boolean prevEscape = false;
        int result = -1;
        int firstNonSpaceIndex = -1;
        int lastNonSpaceIndex = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (prevEscape) {
                key.append(ch);
                prevEscape = false;
            }
            else if (ch == '\\') {
                prevEscape = true;
            }
            else if (ch == '=') {
                result = i + 1;
                break;
            }
            else {
                key.append(ch);
                if (ch > ' ') {
                    int currentLength = key.length();
                    if (firstNonSpaceIndex < 0) {
                        firstNonSpaceIndex = currentLength - 1;
                    }
                    lastNonSpaceIndex = currentLength;
                }
                prevEscape = false;
            }
        }

        key.setLength(lastNonSpaceIndex);

        if (firstNonSpaceIndex > 0) {
            key.delete(0, firstNonSpaceIndex);
        }
        else if (firstNonSpaceIndex < 0) {
            key.setLength(0);
        }

        return result;
    }

    private static CustomVariable parse(String line) {
        StringBuilder key = new StringBuilder();
        int valueStartIndex = parseKey(line, key);
        if (valueStartIndex < 0) {
            return new CustomVariable(key.toString(), "");
        }
        else {
            return new CustomVariable(key.toString(), line.substring(valueStartIndex));
        }
    }

    private static CustomVariables fromVariables(String propertiesStr) {
        List<CustomVariable> vars = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new StringReader(propertiesStr));
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                vars.add(parse(line));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return new MemCustomVariables(vars);
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<CustomVariables> customVariablesRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            customVariablesRef = NbGradleCommonProperties.customVariables(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, CustomVariablesPanel.this);
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;
        private final CustomVariables customVariables;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.customVariables = properties.customVariablesRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, CustomVariablesPanel panel) {
            this.properties = properties;
            this.customVariables = fromVariables(panel.jPropertiesEdit.getText());
        }

        @Override
        public void displaySettings() {
            jPropertiesEdit.setText(toProperties(customVariables));
        }

        @Override
        public void saveSettings() {
            properties.customVariablesRef.setValue(customVariables);
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

        jCustomVariablesCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPropertiesEdit = new javax.swing.JTextArea();
        jCustomVariablesDescr = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jCustomVariablesCaption, org.openide.util.NbBundle.getMessage(CustomVariablesPanel.class, "CustomVariablesPanel.jCustomVariablesCaption.text")); // NOI18N

        jPropertiesEdit.setColumns(20);
        jPropertiesEdit.setRows(5);
        jScrollPane1.setViewportView(jPropertiesEdit);

        org.openide.awt.Mnemonics.setLocalizedText(jCustomVariablesDescr, org.openide.util.NbBundle.getMessage(CustomVariablesPanel.class, "CustomVariablesPanel.jCustomVariablesDescr.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jCustomVariablesCaption)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jCustomVariablesDescr, javax.swing.GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCustomVariablesCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCustomVariablesDescr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jCustomVariablesCaption;
    private javax.swing.JLabel jCustomVariablesDescr;
    private javax.swing.JTextArea jPropertiesEdit;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
