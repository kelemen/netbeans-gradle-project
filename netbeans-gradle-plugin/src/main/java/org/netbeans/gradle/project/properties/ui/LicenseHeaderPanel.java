package org.netbeans.gradle.project.properties.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.DefaultComboBoxModel;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.netbeans.gradle.project.NbGradleProject;
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
import org.netbeans.gradle.project.license.LicenseHeaderInfo;
import org.netbeans.gradle.project.license.LicenseRef;
import org.netbeans.gradle.project.license.LicenseSource;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileChooserBuilder;

import static org.jtrim2.property.BoolProperties.*;
import static org.jtrim2.property.swing.AutoDisplayState.*;

@SuppressWarnings("serial")
public class LicenseHeaderPanel extends javax.swing.JPanel implements ProfileEditorFactory {
    private static final CustomizerCategoryId CATEGORY_ID = new CustomizerCategoryId(
            LicenseHeaderPanel.class.getName() + ".settings",
            NbStrings.getGradleProjectLicenseCategoryName());

    private static final String ORGANIZATION_PROPERTY_NAME = "organization";

    private final Supplier<? extends Path> defaultDirProvider;
    private final LicenseSource licenseSource;
    private String lastDisplayedCustomName;
    private LicenseHeaderInfo unknownLicense;

    private LicenseHeaderPanel(
            Supplier<? extends Path> defaultDirProvider,
            LicenseSource licenseSource) {
        this.defaultDirProvider = Objects.requireNonNull(defaultDirProvider, "defaultDirProvider");
        this.licenseSource = Objects.requireNonNull(licenseSource, "licenseSource");

        this.lastDisplayedCustomName = null;
        this.unknownLicense = null;

        initComponents();

        updateCombo(Arrays.asList(LicenseComboItem.NO_LICENSE, LicenseComboItem.CUSTOM_LICENSE));

        setupEnableDisable();
    }

    private void setupEnableDisable() {
        PropertySource<LicenseComboItem> selectedLicense = SwingProperties.comboBoxSelection(jLicenseCombo);

        addSwingStateListener(equalsWithConst(selectedLicense, LicenseComboItem.CUSTOM_LICENSE),
                componentDisabler(jLicenseTemplateEdit, jBrowseButton));
    }

    public static ProfileBasedSettingsCategory createSettingsCategory(
            NbGradleProject project,
            LicenseSource licenseSource) {
        return createSettingsCategory(toDefaultDirProvider(project), licenseSource);
    }

    public static ProfileBasedSettingsCategory createSettingsCategory(
            final Supplier<? extends Path> defaultDirProvider,
            final LicenseSource licenseSource) {
        Objects.requireNonNull(defaultDirProvider, "defaultDirProvider");
        Objects.requireNonNull(licenseSource, "licenseSource");

        return new ProfileBasedSettingsCategory(CATEGORY_ID, () -> {
            return LicenseHeaderPanel.createSettingsPage(defaultDirProvider, licenseSource);
        });
    }

    public static ProfileBasedSettingsPage createSettingsPage(
            Supplier<? extends Path> defaultDirProvider,
            LicenseSource licenseSource) {
        LicenseHeaderPanel result = new LicenseHeaderPanel(defaultDirProvider, licenseSource);
        return new ProfileBasedSettingsPage(result, result, result.asyncInitTask());
    }

    private static Supplier<? extends Path> toDefaultDirProvider(final NbGradleProject project) {
        Objects.requireNonNull(project, "project");
        return () -> project.currentModel().getValue().getSettingsDir();
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(profileQuery);
    }

    private List<LicenseComboItem> getAllNonDynamicLicenses() throws IOException {
        List<LicenseComboItem> result = new ArrayList<>();
        for (LicenseRef ref: licenseSource.getAllLicense()) {
            if (!ref.isDynamic()) {
                result.add(new LicenseComboItem(ref));
            }
        }

        result.sort(Comparator.comparing(Object::toString, StringUtils.STR_CMP::compare));
        return result;
    }

    private CancelableFunction<Runnable> asyncInitTask() {
        return (CancellationToken cancelToken) -> {
            List<LicenseComboItem> builtInLicenses = getAllNonDynamicLicenses();

            List<LicenseComboItem> items = new ArrayList<>(builtInLicenses.size() + 2);
            items.add(LicenseComboItem.NO_LICENSE);
            items.add(LicenseComboItem.CUSTOM_LICENSE);
            items.addAll(builtInLicenses);

            return updateComboTask(items);
        };
    }

    private LicenseComboItem getSelectedComboItem(LicenseComboItem defaultValue) {
        LicenseComboItem result = (LicenseComboItem)jLicenseCombo.getSelectedItem();
        return result != null ? result : defaultValue;
    }

    private void updateCombo(List<LicenseComboItem> items) {
        LicenseHeaderInfo selection = getLicenseHeaderInfo();

        jLicenseCombo.setModel(new DefaultComboBoxModel<>(items.toArray(new LicenseComboItem[items.size()])));
        displayLicenseHeaderInfo(selection);
    }

    private Runnable updateComboTask(final List<LicenseComboItem> items) {
        return () -> updateCombo(items);
    }

    private void displayLicenseHeaderInfo(final LicenseHeaderInfo info) {
        lastDisplayedCustomName = null;
        unknownLicense = null;

        if (info == null) {
            jLicenseTemplateEdit.setText("");
            jOrganizationEdit.setText("");
            jLicenseCombo.setSelectedItem(LicenseComboItem.NO_LICENSE);
        }
        else {
            String organization = info.getProperties().get(ORGANIZATION_PROPERTY_NAME);
            jOrganizationEdit.setText(organization != null ? organization : "");

            Path licenseTemplate = info.getLicenseTemplateFile();
            jLicenseTemplateEdit.setText(licenseTemplate != null ? licenseTemplate.toString() : "");

            if (licenseTemplate != null) {
                lastDisplayedCustomName = info.getLicenseName();
                jLicenseCombo.setSelectedItem(LicenseComboItem.CUSTOM_LICENSE);
                jLicenseTemplateEdit.setText(licenseTemplate.toString());
            }
            else {
                jLicenseTemplateEdit.setText("");

                LicenseRef licenseRef = new LicenseRef(info.getLicenseName(), "", false);
                LicenseComboItem newSelection = new LicenseComboItem(licenseRef);
                jLicenseCombo.setSelectedItem(newSelection);
                if (!newSelection.equals(getSelectedComboItem(null))) {
                    // TODO: We should somehow display this state to the user, otherwise he
                    //       can't just select no license.
                    //       Note that this is not a big issue because an unknown license
                    //       will act the same way as having no license selected.
                    unknownLicense = info;
                    jLicenseCombo.setSelectedItem(LicenseComboItem.NO_LICENSE);
                }
            }
        }
    }

    private LicenseHeaderInfo getLicenseHeaderInfo() {
        LicenseComboItem selected = getSelectedComboItem(LicenseComboItem.NO_LICENSE);
        if (selected.equals(LicenseComboItem.NO_LICENSE)) {
            return unknownLicense;
        }

        String organization = jOrganizationEdit.getText().trim();
        Map<String, String> properties = Collections.singletonMap(ORGANIZATION_PROPERTY_NAME, organization);

        if (selected.equals(LicenseComboItem.CUSTOM_LICENSE)) {
            String template = jLicenseTemplateEdit.getText().trim();
            if (template.isEmpty()) {
                return null;
            }

            String name = lastDisplayedCustomName != null
                    ? lastDisplayedCustomName
                    : "Custom";
            return new LicenseHeaderInfo(name, properties, Paths.get(template));
        }

        String licenseId = selected.getLicenseId();
        return new LicenseHeaderInfo(licenseId, properties, null);
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<LicenseHeaderInfo> licenseHeaderInfoRef;

        public PropertyRefs(ActiveSettingsQuery settingsQuery) {
            this.licenseHeaderInfoRef = NbGradleCommonProperties.licenseHeaderInfo(settingsQuery);
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, LicenseHeaderPanel.this);
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;
        private final LicenseHeaderInfo licenseHeaderInfo;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.licenseHeaderInfo = properties.licenseHeaderInfoRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, LicenseHeaderPanel panel) {
            this.properties = properties;
            this.licenseHeaderInfo = panel.getLicenseHeaderInfo();
        }

        @Override
        public void displaySettings() {
            displayLicenseHeaderInfo(licenseHeaderInfo);
        }

        @Override
        public void saveSettings() {
            properties.licenseHeaderInfoRef.setValue(licenseHeaderInfo);
        }
    }

    private static final class LicenseComboItem {
        private static final LicenseComboItem NO_LICENSE = new LicenseComboItem(false);
        private static final LicenseComboItem CUSTOM_LICENSE = new LicenseComboItem(true);

        private final LicenseRef licenseRef;
        private final boolean hasLicense;

        public LicenseComboItem(LicenseRef licenseRef) {
            this(licenseRef, true);
        }

        public LicenseComboItem(boolean hasLicense) {
            this(null, hasLicense);
        }

        public LicenseComboItem(LicenseRef licenseRef, boolean hasLicense) {
            this.licenseRef = licenseRef;
            this.hasLicense = hasLicense;
        }

        public String getLicenseId() {
            return licenseRef != null ? licenseRef.getId() : null;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(getLicenseId());
            hash = 41 * hash + (this.hasLicense ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final LicenseComboItem other = (LicenseComboItem)obj;
            if (this.hasLicense != other.hasLicense) return false;
            return this.hasLicense == other.hasLicense
                    && Objects.equals(this.getLicenseId(), other.getLicenseId());
        }

        @Override
        public String toString() {
            if (licenseRef != null) {
                return licenseRef.getDisplayName();
            }

            // TODO: I18N
            return hasLicense ? "Custom license" : "No license";
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

        jOrganizationCaption = new javax.swing.JLabel();
        jOrganizationEdit = new javax.swing.JTextField();
        jLicenseNameCaption = new javax.swing.JLabel();
        jLicenseTemplateCaption = new javax.swing.JLabel();
        jLicenseTemplateEdit = new javax.swing.JTextField();
        jBrowseButton = new javax.swing.JButton();
        jCaption = new javax.swing.JLabel();
        jLicenseCombo = new javax.swing.JComboBox<>();

        org.openide.awt.Mnemonics.setLocalizedText(jOrganizationCaption, org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jOrganizationCaption.text")); // NOI18N

        jOrganizationEdit.setText(org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jOrganizationEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLicenseNameCaption, org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jLicenseNameCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLicenseTemplateCaption, org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jLicenseTemplateCaption.text")); // NOI18N

        jLicenseTemplateEdit.setText(org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jLicenseTemplateEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jBrowseButton, org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jBrowseButton.text")); // NOI18N
        jBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowseButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jCaption, org.openide.util.NbBundle.getMessage(LicenseHeaderPanel.class, "LicenseHeaderPanel.jCaption.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCaption, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jOrganizationEdit)
                    .addComponent(jLicenseCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLicenseTemplateEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBrowseButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jOrganizationCaption)
                            .addComponent(jLicenseNameCaption)
                            .addComponent(jLicenseTemplateCaption))
                        .addGap(0, 360, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCaption, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jOrganizationCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jOrganizationEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLicenseNameCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLicenseCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLicenseTemplateCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLicenseTemplateEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowseButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private File tryGetDefaultDir() {
        Path result = defaultDirProvider.get();
        return result != null ? result.toFile() : null;
    }

    private void jBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowseButtonActionPerformed
        File defaultDir = tryGetDefaultDir();
        File initialDir = defaultDir;

        FileChooserBuilder dlgChooser = new FileChooserBuilder(
                LicenseHeaderPanel.class.getName() + (initialDir != null ? ("-" + initialDir.getName()) : ""));
        dlgChooser.setDefaultWorkingDirectory(initialDir);

        File f = dlgChooser.showOpenDialog();
        if (f == null || f.isDirectory()) {
            return;
        }

        File file = f.getAbsoluteFile();
        String relPath = defaultDir != null
                ? NbFileUtils.tryMakeRelative(defaultDir, file)
                : null;
        jLicenseTemplateEdit.setText(relPath != null ? relPath : file.getPath());

    }//GEN-LAST:event_jBrowseButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBrowseButton;
    private javax.swing.JLabel jCaption;
    private javax.swing.JComboBox<LicenseComboItem> jLicenseCombo;
    private javax.swing.JLabel jLicenseNameCaption;
    private javax.swing.JLabel jLicenseTemplateCaption;
    private javax.swing.JTextField jLicenseTemplateEdit;
    private javax.swing.JLabel jOrganizationCaption;
    private javax.swing.JTextField jOrganizationEdit;
    // End of variables declaration//GEN-END:variables
}
