package org.netbeans.gradle.project.properties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileValuesEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileValuesEditorFactory;
import org.netbeans.gradle.project.properties.standard.CustomVariable;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.properties.standard.MemCustomVariables;
import org.netbeans.gradle.project.util.StringUtils;

@SuppressWarnings("serial")
public class CustomVariablesPanel extends javax.swing.JPanel {
    public CustomVariablesPanel() {
        initComponents();
    }

    public static ProfileBasedPanel createProfileBasedPanel(final NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        final CustomVariablesPanel customPanel = new CustomVariablesPanel();
        return ProfileBasedPanel.createPanel(project, customPanel, new ProfileValuesEditorFactory() {
            @Override
            public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery) {
                return customPanel.new PropertyValues(profileQuery);
            }
        });
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

    public static CustomVariables fromVariables(String propertiesStr) {
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

    private final class PropertyValues implements ProfileValuesEditor {
        public final PropertyReference<CustomVariables> customVariablesRef;
        private CustomVariables customVariables;

        public PropertyValues(ActiveSettingsQuery settings) {
            this.customVariablesRef = NbGradleCommonProperties.customVariables(settings);
            this.customVariables = customVariablesRef.tryGetValueWithoutFallback();
        }

        @Override
        public void displayValues() {
            jPropertiesEdit.setText(toProperties(customVariables));
        }

        @Override
        public void readFromGui() {
            customVariables = fromVariables(jPropertiesEdit.getText());
        }

        @Override
        public void applyValues() {
            customVariablesRef.setValue(customVariables);
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
