package org.netbeans.gradle.project.properties.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.PlatformSelectionMode;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.standard.PlatformId;
import org.netbeans.gradle.project.util.NbFileUtils;

@SuppressWarnings("serial")
public class ScriptAndTasksPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Script-and-Tasks");

    public ScriptAndTasksPanel() {
        initComponents();
    }

    public static GlobalSettingsPage createSettingsPage() {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new ScriptAndTasksPanel());
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    private void fillPlatformCombo() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        JavaPlatformItem[] comboItems = new JavaPlatformItem[platforms.length];
        for (int i = 0; i < platforms.length; i++) {
            comboItems[i] = new JavaPlatformItem(platforms[i]);
        }

        jJdkCombo.setModel(new DefaultComboBoxModel<>(comboItems));
    }

    private static String argListToText(List<String> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(256);
        for (String arg: args) {
            result.append(arg);
            result.append('\n');
        }
        return result.toString();
    }

    private static List<String> textToArgsList(String str) {
        try {
            return textToArgsListUnsafe(str);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<String> textToArgsListUnsafe(String str) throws IOException {
        List<String> result = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new StringReader(str));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (!line.trim().isEmpty()) {
                result.add(line);
            }
        }

        return result;
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private void displayGradleArgs(List<String> args) {
        jGradleArgs.setText(argListToText(args));
    }

    private void displayGradleJvmArgs(List<String> jvmArgs) {
        jGradleJVMArgs.setText(argListToText(jvmArgs));
    }

    private void displayScriptPlatform(ScriptPlatform jdk) {
        fillPlatformCombo();

        if (jdk != null) {
            jJdkCombo.setSelectedItem(new JavaPlatformItem(jdk.getJavaPlatform()));
        }
    }

    private ScriptPlatform getScriptJdk() {
        JavaPlatform result = getJdk();
        return result != null ? new ScriptPlatform(result, PlatformSelectionMode.BY_LOCATION) : null;
    }

    private JavaPlatform getJdk() {
        @SuppressWarnings("unchecked")
        JavaPlatformItem selected = (JavaPlatformItem)jJdkCombo.getSelectedItem();
        return selected != null ? selected.getPlatform() : JavaPlatform.getDefault();
    }

    private String getGradleArgs() {
        String result = jGradleArgs.getText();
        return result != null ? result.trim() : "";
    }

    private String getGradleJvmArgs() {
        String result = jGradleJVMArgs.getText();
        return result != null ? result.trim() : "";
    }

    private static class JavaPlatformItem {
        private final JavaPlatform platform;
        private final String displayName;

        public JavaPlatformItem(JavaPlatform platform) {
            ExceptionHelper.checkNotNullArgument(platform, "platform");
            this.platform = platform;
            this.displayName = PlatformId.getDisplayNameOfPlatform(platform);
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        @Override
        public String toString() {
            return displayName;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.platform.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            final JavaPlatformItem other = (JavaPlatformItem)obj;
            return Objects.equals(this.platform, other.platform);
        }
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<ScriptPlatform> defaultJdkRef;
        private final PropertyReference<List<String>> gradleArgsRef;
        private final PropertyReference<List<String>> gradleJvmArgsRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            defaultJdkRef = CommonGlobalSettings.defaultJdk(settingsQuery);
            gradleArgsRef = CommonGlobalSettings.gradleArgs(settingsQuery);
            gradleJvmArgsRef = CommonGlobalSettings.gradleJvmArgs(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, ScriptAndTasksPanel.this);
        }
    }

    private class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final ScriptPlatform defaultJdk;
        private final List<String> gradleArgs;
        private final List<String> gradleJvmArgs;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;

            this.defaultJdk = properties.defaultJdkRef.tryGetValueWithoutFallback();
            this.gradleArgs = properties.gradleArgsRef.tryGetValueWithoutFallback();
            this.gradleJvmArgs = properties.gradleJvmArgsRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, ScriptAndTasksPanel panel) {
            this.properties = properties;

            this.defaultJdk = panel.getScriptJdk();
            this.gradleArgs = textToArgsList(panel.getGradleArgs());
            this.gradleJvmArgs = textToArgsList(panel.getGradleJvmArgs());
        }

        @Override
        public void displaySettings() {
            displayGradleArgs(gradleArgs);
            displayGradleJvmArgs(gradleJvmArgs);
            displayScriptPlatform(defaultJdk != null
                    ? defaultJdk
                    : properties.defaultJdkRef.getActiveValue());
        }

        @Override
        public void saveSettings() {
            properties.defaultJdkRef.setValue(defaultJdk);
            properties.gradleArgsRef.setValue(gradleArgs);
            properties.gradleJvmArgsRef.setValue(gradleJvmArgs);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jGradleJVMArgs = new javax.swing.JTextArea();
        jGradleVMArgsCaption = new javax.swing.JLabel();
        jJdkCombo = new javax.swing.JComboBox<>();
        jGradleJdkCaption = new javax.swing.JLabel();
        jGradleArgsCaption = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jGradleArgs = new javax.swing.JTextArea();

        jGradleJVMArgs.setColumns(20);
        jGradleJVMArgs.setRows(3);
        jScrollPane1.setViewportView(jGradleJVMArgs);

        org.openide.awt.Mnemonics.setLocalizedText(jGradleVMArgsCaption, org.openide.util.NbBundle.getMessage(ScriptAndTasksPanel.class, "ScriptAndTasksPanel.jGradleVMArgsCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleJdkCaption, org.openide.util.NbBundle.getMessage(ScriptAndTasksPanel.class, "ScriptAndTasksPanel.jGradleJdkCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleArgsCaption, org.openide.util.NbBundle.getMessage(ScriptAndTasksPanel.class, "ScriptAndTasksPanel.jGradleArgsCaption.text")); // NOI18N

        jGradleArgs.setColumns(20);
        jGradleArgs.setRows(3);
        jScrollPane2.setViewportView(jGradleArgs);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jJdkCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jGradleJdkCaption)
                            .addComponent(jGradleVMArgsCaption)
                            .addComponent(jGradleArgsCaption))
                        .addGap(0, 65, Short.MAX_VALUE))
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jGradleJdkCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jJdkCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleArgsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleVMArgsCaption, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea jGradleArgs;
    private javax.swing.JLabel jGradleArgsCaption;
    private javax.swing.JTextArea jGradleJVMArgs;
    private javax.swing.JLabel jGradleJdkCaption;
    private javax.swing.JLabel jGradleVMArgsCaption;
    private javax.swing.JComboBox<JavaPlatformItem> jJdkCombo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}
