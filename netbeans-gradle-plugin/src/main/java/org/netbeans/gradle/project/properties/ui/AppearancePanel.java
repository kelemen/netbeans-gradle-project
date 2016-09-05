package org.netbeans.gradle.project.properties.ui;

import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsEditor;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.netbeans.gradle.project.properties.global.SettingsEditorProperties;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;

@SuppressWarnings("serial")
public class AppearancePanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Appearance");

    private final ProjectNodeNamePanel nodeNamePanel;

    public AppearancePanel() {
        initComponents();

        nodeNamePanel = new ProjectNodeNamePanel(false);
        jProjectNodeNameHolder.add(nodeNamePanel);
        fillJavaSourcesDisplayModeCombo();
    }

    private void fillJavaSourcesDisplayModeCombo() {
        jSourcesDisplayMode.removeAllItems();

        JavaSourcesDisplayMode[] displayModes = JavaSourcesDisplayMode.values();
        SourcesDisplayModeItem[] items = new SourcesDisplayModeItem[displayModes.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = new SourcesDisplayModeItem(displayModes[i]);
        }

        Arrays.sort(items, new Comparator<SourcesDisplayModeItem>() {
            @Override
            public int compare(SourcesDisplayModeItem o1, SourcesDisplayModeItem o2) {
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
            }
        });

        for (SourcesDisplayModeItem item: items) {
            jSourcesDisplayMode.addItem(item);
        }
    }

    @Override
    public void updateSettings(ActiveSettingsQuery globalSettings) {
        PropertyReference<String> displayNamePattern = NbGradleCommonProperties.displayNamePattern(globalSettings);
        PropertyReference<JavaSourcesDisplayMode> javaSourcesDisplayMode = CommonGlobalSettings.javaSourcesDisplayMode(globalSettings);

        nodeNamePanel.updatePattern(displayNamePattern.getActiveValue(), null);
        selectSourcesDisplayMode(javaSourcesDisplayMode.getActiveValue());
    }

    private void selectSourcesDisplayMode(JavaSourcesDisplayMode newMode) {
        jSourcesDisplayMode.setSelectedItem(new SourcesDisplayModeItem(newMode));
    }

    @Override
    public void saveSettings(ActiveSettingsQuery globalSettings) {
        PropertyReference<String> displayNamePattern = NbGradleCommonProperties.displayNamePattern(globalSettings);
        PropertyReference<JavaSourcesDisplayMode> javaSourcesDisplayMode = CommonGlobalSettings.javaSourcesDisplayMode(globalSettings);

        displayNamePattern.setValue(nodeNamePanel.getNamePattern());
        javaSourcesDisplayMode.setValue(javaSourcesDisplayMode());
    }

    private JavaSourcesDisplayMode javaSourcesDisplayMode() {
        SourcesDisplayModeItem selected = (SourcesDisplayModeItem)jSourcesDisplayMode.getSelectedItem();
        if (selected == null) {
            return JavaSourcesDisplayMode.DEFAULT_MODE;
        }
        return selected.displayMode;
    }

    @Override
    public SettingsEditorProperties getProperties() {
        SettingsEditorProperties.Builder result = new SettingsEditorProperties.Builder(this);
        result.setHelpUrl(HELP_URL);

        return result.create();
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
        jPanel1 = new javax.swing.JPanel();
        jSourcesDisplayCaption = new javax.swing.JLabel();
        jSourcesDisplayMode = new javax.swing.JComboBox<>();

        jProjectNodeNameHolder.setLayout(new java.awt.GridLayout(1, 1));

        org.openide.awt.Mnemonics.setLocalizedText(jSourcesDisplayCaption, org.openide.util.NbBundle.getMessage(AppearancePanel.class, "AppearancePanel.jSourcesDisplayCaption.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSourcesDisplayCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSourcesDisplayMode, 0, 257, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourcesDisplayCaption)
                    .addComponent(jSourcesDisplayMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jProjectNodeNameHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jProjectNodeNameHolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jProjectNodeNameHolder;
    private javax.swing.JLabel jSourcesDisplayCaption;
    private javax.swing.JComboBox<SourcesDisplayModeItem> jSourcesDisplayMode;
    // End of variables declaration//GEN-END:variables
}
