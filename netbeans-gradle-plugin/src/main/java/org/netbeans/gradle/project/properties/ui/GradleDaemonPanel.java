package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.swing.SpinnerNumberModel;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbFileUtils;

import static org.jtrim2.property.BoolProperties.*;
import static org.jtrim2.property.swing.AutoDisplayState.*;
import static org.jtrim2.property.swing.SwingProperties.*;

@SuppressWarnings("serial")
public class GradleDaemonPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Gradle-Daemon");

    private static final int DEFAULT_TIMEOUT_SEC = longToInt(TimeUnit.HOURS.toSeconds(3));
    private static final TimeUnit DISPLAY_UNIT = TimeUnit.MINUTES;

    public GradleDaemonPanel() {
        initComponents();

        jDaemonTimeoutSpinner.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 30));

        setupEnableDisable();
    }

    public static GlobalSettingsPage createSettingsPage() {
        GlobalSettingsPage.Builder result = new GlobalSettingsPage.Builder(new GradleDaemonPanel());
        result.setHelpUrl(HELP_URL);
        return result.create();
    }

    private void setupEnableDisable() {
        addSwingStateListener(not(buttonSelected(jUseDefaultDaemonTimeoutCheck)),
                componentDisabler(jDaemonTimeoutSpinner));
    }

    private int getDisplayedTimeout(PropertyRefs properties, Integer seconds) {
        if (seconds != null) {
            return seconds;
        }

        Integer active = properties.gradleDaemonTimeoutSecRef.getActiveValue();
        return active != null ? active : DEFAULT_TIMEOUT_SEC;
    }

    private void displayDaemonTimeout(PropertyRefs properties, Integer seconds) {
        jUseDefaultDaemonTimeoutCheck.setSelected(seconds == null);
        int displayTimeoutSec = getDisplayedTimeout(properties, seconds);

        jDaemonTimeoutSpinner.setValue(longToInt(DISPLAY_UNIT.convert(displayTimeoutSec, TimeUnit.SECONDS)));
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private static int longToInt(long value) {
        if (value >= (long)Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value <= (long)Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int)value;
    }

    private Integer getDaemonTimeoutInSec(PropertyRefs properties) {
        if (jUseDefaultDaemonTimeoutCheck.isSelected()) {
            return null;
        }

        Object value = jDaemonTimeoutSpinner.getValue();

        int displayTimeout = -1;
        if (value instanceof Number) {
            displayTimeout = ((Number)value).intValue();
        }

        return displayTimeout >= 0
                ? longToInt(DISPLAY_UNIT.toSeconds(displayTimeout))
                : properties.gradleDaemonTimeoutSecRef.tryGetValueWithoutFallback();
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<Integer> gradleDaemonTimeoutSecRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            gradleDaemonTimeoutSecRef = CommonGlobalSettings.gradleDaemonTimeoutSec(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, GradleDaemonPanel.this);
        }
    }

    private class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final Integer gradleDaemonTimeoutSec;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.gradleDaemonTimeoutSec = properties.gradleDaemonTimeoutSecRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, GradleDaemonPanel panel) {
            this.properties = properties;
            this.gradleDaemonTimeoutSec = panel.getDaemonTimeoutInSec(properties);
        }

        @Override
        public void displaySettings() {
            displayDaemonTimeout(properties, gradleDaemonTimeoutSec);
        }

        @Override
        public void saveSettings() {
            properties.gradleDaemonTimeoutSecRef.setValue(gradleDaemonTimeoutSec);
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

        jDaemonTimeoutCaption = new javax.swing.JLabel();
        jDaemonTimeoutSpinner = new javax.swing.JSpinner();
        jUseDefaultDaemonTimeoutCheck = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jDaemonTimeoutCaption, org.openide.util.NbBundle.getMessage(GradleDaemonPanel.class, "GradleDaemonPanel.jDaemonTimeoutCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jUseDefaultDaemonTimeoutCheck, org.openide.util.NbBundle.getMessage(GradleDaemonPanel.class, "GradleDaemonPanel.jUseDefaultDaemonTimeoutCheck.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDaemonTimeoutCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jDaemonTimeoutSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jUseDefaultDaemonTimeoutCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDaemonTimeoutCaption)
                    .addComponent(jDaemonTimeoutSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jUseDefaultDaemonTimeoutCheck))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jDaemonTimeoutCaption;
    private javax.swing.JSpinner jDaemonTimeoutSpinner;
    private javax.swing.JCheckBox jUseDefaultDaemonTimeoutCheck;
    // End of variables declaration//GEN-END:variables
}
