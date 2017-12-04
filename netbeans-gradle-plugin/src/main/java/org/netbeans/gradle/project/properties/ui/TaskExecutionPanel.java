package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import javax.swing.JCheckBox;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.SelfMaintainedTasks;
import org.netbeans.gradle.project.util.NbFileUtils;

@SuppressWarnings("serial")
public class TaskExecutionPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Task-Execution");

    private final EnumCombo<SelfMaintainedTasks> selfMaintainedTasksCombo;

    public TaskExecutionPanel() {
        initComponents();

        selfMaintainedTasksCombo = new EnumCombo<>(SelfMaintainedTasks.class, SelfMaintainedTasks.FALSE, jAutoTasks);
    }

    public static GlobalSettingsPage createSettingsPage() {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new TaskExecutionPanel());
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private void displayCheck(JCheckBox checkbox, Boolean value, PropertyReference<Boolean> propertyRef) {
        displayCheck(checkbox, value != null ? value : propertyRef.getActiveValue());
    }

    private void displayCheck(JCheckBox checkbox, Boolean value) {
        if (value != null) {
            checkbox.setSelected(value);
        }
    }

    private void displaySelfMaintainedTask(SelfMaintainedTasks value) {
        if (value != null) {
            selfMaintainedTasksCombo.setSelectedValue(value);
        }
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<Boolean> alwaysClearOutputRef;
        private final PropertyReference<SelfMaintainedTasks> selfMaintainedTasksRef;
        private final PropertyReference<Boolean> skipTestsRef;
        private final PropertyReference<Boolean> skipCheckRef;
        private final PropertyReference<Boolean> replaceLfOnStdInRef;
        private final PropertyReference<Boolean> askBeforeCancelExecRef;
        private final PropertyReference<Boolean> showGradleVersionRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            alwaysClearOutputRef = CommonGlobalSettings.alwaysClearOutput(settingsQuery);
            selfMaintainedTasksRef = CommonGlobalSettings.selfMaintainedTasks(settingsQuery);
            skipTestsRef = CommonGlobalSettings.skipTests(settingsQuery);
            skipCheckRef = CommonGlobalSettings.skipCheck(settingsQuery);
            replaceLfOnStdInRef = CommonGlobalSettings.replaceLfOnStdIn(settingsQuery);
            askBeforeCancelExecRef = CommonGlobalSettings.askBeforeCancelExec(settingsQuery);
            showGradleVersionRef = CommonGlobalSettings.showGradleVersion(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, TaskExecutionPanel.this);
        }
    }

    private class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final Boolean alwaysClearOutput;
        private final SelfMaintainedTasks selfMaintainedTasks;
        private final Boolean skipTests;
        private final Boolean skipCheck;
        private final Boolean replaceLfOnStdIn;
        private final Boolean askBeforeCancelExec;
        private final Boolean showGradleVerison;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;

            this.alwaysClearOutput = properties.alwaysClearOutputRef.tryGetValueWithoutFallback();
            this.selfMaintainedTasks = properties.selfMaintainedTasksRef.tryGetValueWithoutFallback();
            this.skipTests = properties.skipTestsRef.tryGetValueWithoutFallback();
            this.skipCheck = properties.skipCheckRef.tryGetValueWithoutFallback();
            this.replaceLfOnStdIn = properties.replaceLfOnStdInRef.tryGetValueWithoutFallback();
            this.askBeforeCancelExec = properties.askBeforeCancelExecRef.tryGetValueWithoutFallback();
            this.showGradleVerison = properties.showGradleVersionRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, TaskExecutionPanel panel) {
            this.properties = properties;

            this.alwaysClearOutput = panel.jAlwayClearOutput.isSelected();
            this.selfMaintainedTasks = panel.selfMaintainedTasksCombo.getSelectedValue();
            this.skipTests = panel.jSkipTestsCheck.isSelected();
            this.skipCheck = panel.jSkipCheckCheckBox.isSelected();
            this.replaceLfOnStdIn = panel.jReplaceLfOnStdIn.isSelected();
            this.askBeforeCancelExec = panel.jAskBeforeCancelExecution.isSelected();
            this.showGradleVerison = panel.jShowGradleVersion.isSelected();
        }

        @Override
        public void displaySettings() {
            displaySelfMaintainedTask(selfMaintainedTasks != null
                    ? selfMaintainedTasks
                    : properties.selfMaintainedTasksRef.getActiveValue());
            displayCheck(jAlwayClearOutput, alwaysClearOutput, properties.alwaysClearOutputRef);
            displayCheck(jSkipTestsCheck, skipTests, properties.skipTestsRef);
            displayCheck(jSkipCheckCheckBox, skipCheck, properties.skipCheckRef);
            displayCheck(jReplaceLfOnStdIn, replaceLfOnStdIn, properties.replaceLfOnStdInRef);
            displayCheck(jAskBeforeCancelExecution, askBeforeCancelExec, properties.askBeforeCancelExecRef);
            displayCheck(jShowGradleVersion, showGradleVerison, properties.showGradleVersionRef);
        }

        @Override
        public void saveSettings() {
            properties.alwaysClearOutputRef.setValue(alwaysClearOutput);
            properties.selfMaintainedTasksRef.setValue(selfMaintainedTasks);
            properties.skipTestsRef.setValue(skipTests);
            properties.skipCheckRef.setValue(skipCheck);
            properties.replaceLfOnStdInRef.setValue(replaceLfOnStdIn);
            properties.askBeforeCancelExecRef.setValue(askBeforeCancelExec);
            properties.showGradleVersionRef.setValue(showGradleVerison);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSkipTestsCheck = new javax.swing.JCheckBox();
        jSkipCheckCheckBox = new javax.swing.JCheckBox();
        jAlwayClearOutput = new javax.swing.JCheckBox();
        jReplaceLfOnStdIn = new javax.swing.JCheckBox();
        jAutoTasksCaption = new javax.swing.JLabel();
        jAutoTasks = new javax.swing.JComboBox<>();
        jAskBeforeCancelExecution = new javax.swing.JCheckBox();
        jShowGradleVersion = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jSkipTestsCheck, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jSkipTestsCheck.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSkipCheckCheckBox, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jSkipCheckCheckBox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAlwayClearOutput, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jAlwayClearOutput.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jReplaceLfOnStdIn, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jReplaceLfOnStdIn.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAutoTasksCaption, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jAutoTasksCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAskBeforeCancelExecution, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jAskBeforeCancelExecution.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jShowGradleVersion, org.openide.util.NbBundle.getMessage(TaskExecutionPanel.class, "TaskExecutionPanel.jShowGradleVersion.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jAutoTasksCaption)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jAutoTasks, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jAskBeforeCancelExecution)
                            .addComponent(jSkipTestsCheck)
                            .addComponent(jAlwayClearOutput)
                            .addComponent(jSkipCheckCheckBox)
                            .addComponent(jReplaceLfOnStdIn)
                            .addComponent(jShowGradleVersion))
                        .addGap(0, 46, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSkipTestsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSkipCheckCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jAlwayClearOutput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jReplaceLfOnStdIn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jAskBeforeCancelExecution)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jShowGradleVersion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAutoTasksCaption)
                    .addComponent(jAutoTasks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jAlwayClearOutput;
    private javax.swing.JCheckBox jAskBeforeCancelExecution;
    private javax.swing.JComboBox<EnumCombo.Item<SelfMaintainedTasks>> jAutoTasks;
    private javax.swing.JLabel jAutoTasksCaption;
    private javax.swing.JCheckBox jReplaceLfOnStdIn;
    private javax.swing.JCheckBox jShowGradleVersion;
    private javax.swing.JCheckBox jSkipCheckCheckBox;
    private javax.swing.JCheckBox jSkipTestsCheck;
    // End of variables declaration//GEN-END:variables
}
