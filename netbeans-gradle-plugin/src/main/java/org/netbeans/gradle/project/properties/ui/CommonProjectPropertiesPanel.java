package org.netbeans.gradle.project.properties.ui;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.CustomizerCategoryId;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPageFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationRef;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.PlatformSelectionMode;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.properties.standard.PlatformId;
import org.netbeans.gradle.project.properties.standard.SourceEncodingProperty;
import org.netbeans.gradle.project.properties.standard.UserInitScriptPath;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbGuiUtils;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Lookup;

@SuppressWarnings("serial")
public class CommonProjectPropertiesPanel extends JPanel implements ProfileEditorFactory {
    public static final CustomizerCategoryId CATEGORY_ID = new CustomizerCategoryId(
            CommonProjectPropertiesPanel.class.getName() + ".settings",
            NbStrings.getGradleProjectCategoryName());

    private final NbGradleProject project;

    private PropertyRefs currentValues;
    private GradleLocationDef selectedGradleLocation;

    private CommonProjectPropertiesPanel(NbGradleProject project) {
        this.project = project;

        currentValues = null;
        selectedGradleLocation = null;

        initComponents();

        @SuppressWarnings("unchecked")
        ComboBoxModel<Charset> sourceEncodingModel = (ComboBoxModel<Charset>)ProjectCustomizer.encodingModel(
                SourceEncodingProperty.DEFAULT_SOURCE_ENCODING.name());
        jSourceEncoding.setModel(sourceEncodingModel);

        setupEnableDisable();
    }

    public static ProfileBasedSettingsCategory createSettingsCategory(final NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        return new ProfileBasedSettingsCategory(CATEGORY_ID, new ProfileBasedSettingsPageFactory() {
            @Override
            public ProfileBasedSettingsPage createSettingsPage() {
                return CommonProjectPropertiesPanel.createSettingsPage(project);
            }
        });
    }

    public static ProfileBasedSettingsPage createSettingsPage(NbGradleProject project) {
        CommonProjectPropertiesPanel result = new CommonProjectPropertiesPanel(project);
        return new ProfileBasedSettingsPage(result, result);
    }

    @Override
    public ProfileEditor startEditingProfile(ProfileInfo profileInfo, ActiveSettingsQuery profileQuery) {
        return new PropertyRefs(project, profileInfo, profileQuery);
    }

    private static <Value> Value setInheritAndGetValue(
            Value value,
            PropertyReference<? extends Value> valueWitFallbacks,
            JCheckBox inheritCheck) {
        inheritCheck.setSelected(value == null);
        return value != null ? value : valueWitFallbacks.getActiveValue();
    }

    private static void setupInheritCheck(final JCheckBox inheritCheck, JComponent... components) {
        NbGuiUtils.enableBasedOnCheck(inheritCheck, false, components);
    }

    private void setupEnableDisable() {
        setupInheritCheck(jScriptPlatformInherit, jScriptPlatformCombo);
        setupInheritCheck(jGradleHomeInherit, jGradleHomeEdit, jGradleHomeChangeButton);
        setupInheritCheck(jPlatformComboInherit, jPlatformCombo);
        setupInheritCheck(jSourceEncodingInherit, jSourceEncoding);
        setupInheritCheck(jSourceLevelComboInherit, jSourceLevelCombo);
        setupInheritCheck(jUserInitScriptInherit, jUserInitScript, jUserInitScriptBrowseButton);
    }

    private void fillProjectPlatformCombo() {
        List<ProjectPlatformComboItem> comboItems = new ArrayList<>();
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            for (ProjectPlatform platform: query.getAvailablePlatforms()) {
                comboItems.add(new ProjectPlatformComboItem(platform));
            }
        }

        jPlatformCombo.setModel(new DefaultComboBoxModel<>(comboItems.toArray(new ProjectPlatformComboItem[comboItems.size()])));
    }

    private void refreshScriptPlatformCombo(boolean selectByVersion) {
        JavaPlatformComboItem currentSelection = (JavaPlatformComboItem)jScriptPlatformCombo.getSelectedItem();

        fillScriptPlatformCombo(selectByVersion);

        jScriptPlatformCombo.setSelectedItem(new JavaPlatformComboItem(
                currentSelection.getPlatform(),
                toSelectionMode(selectByVersion)));
    }

    private void refreshTargetPlatformCombo() {
        Object currentSelection = jPlatformCombo.getSelectedItem();
        fillProjectPlatformCombo();
        jPlatformCombo.setSelectedItem(currentSelection);
    }

    private void refreshPlatformCombos(PropertyRefs properties) {
        refreshScriptPlatformCombo(properties.isSelectByVersion());
        refreshTargetPlatformCombo();
    }

    private static PlatformOrder order() {
        return CommonGlobalSettings.getDefault().platformPreferenceOrder().getActiveValue();
    }

    private List<JavaPlatform> getPlatforms(boolean selectByVersion) {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        if (selectByVersion) {
            return order().filterIndistinguishable(platforms);
        }
        else {
            return Arrays.asList(platforms);
        }
    }

    private static PlatformSelectionMode toSelectionMode(boolean selectByVersion) {
        return selectByVersion ? PlatformSelectionMode.BY_VERSION : PlatformSelectionMode.BY_LOCATION;
    }

    private StringResolver getLocationResolver() {
        return StringResolvers.getDefaultResolverSelector().getProjectResolver(project, Lookup.EMPTY);
    }

    private GradleLocation tryGetLocation(GradleLocationDef locationDef) {
        if (locationDef == null) {
            return null;
        }

        StringResolver resolver = getLocationResolver();
        return locationDef.getLocation(resolver);
    }

    private String getLocationStr(GradleLocationDef locationDef) {
        GradleLocation location = tryGetLocation(locationDef);
        return location != null ? location.toLocalizedString() : "";
    }

    private void selectGradleLocation(GradleLocationDef newLocationDef) {
        selectedGradleLocation = newLocationDef;
        jGradleHomeEdit.setText(getLocationStr(newLocationDef));
    }

    private void fillScriptPlatformCombo(boolean selectByVersion) {
        PlatformSelectionMode selectionMode = toSelectionMode(selectByVersion);

        List<JavaPlatform> platforms = getPlatforms(selectByVersion);
        List<JavaPlatformComboItem> comboItems = new ArrayList<>(platforms.size());

        for (JavaPlatform platform: platforms) {
            Specification specification = platform.getSpecification();
            if (specification != null && specification.getVersion() != null) {
                comboItems.add(new JavaPlatformComboItem(platform, selectionMode));
            }
        }

        jScriptPlatformCombo.setModel(new DefaultComboBoxModel<>(comboItems.toArray(new JavaPlatformComboItem[comboItems.size()])));
    }

    private Charset getSelectedSourceEncoding(PropertyRefs properties) {
        if (jSourceEncodingInherit.isSelected()) {
            return null;
        }

        Object selected = jSourceEncoding.getSelectedItem();
        if (selected instanceof Charset) {
            return (Charset)selected;
        }
        return properties.sourceEncodingRef.tryGetValueWithoutFallback();
    }

    private GradleLocationDef getGradleLocation() {
        GradleLocationDef gradleHome = selectedGradleLocation;
        return jGradleHomeInherit.isSelected() ? null : gradleHome;
    }

    private String getSourceLevel() {
        return jSourceLevelComboInherit.isSelected() ? null : (String)jSourceLevelCombo.getSelectedItem();
    }

    private UserInitScriptPath getUserInitScript() {
        if (jUserInitScriptInherit.isSelected()) {
            return null;
        }

        String userInitScriptStr = jUserInitScript.getText().trim();
        if (userInitScriptStr.isEmpty()) {
            return null;
        }

        return new UserInitScriptPath(Paths.get(userInitScriptStr));
    }

    private ProjectPlatform getTargetPlaform(PropertyRefs properties) {
        if (jPlatformComboInherit.isSelected()) {
            return null;
        }
        ProjectPlatformComboItem selected = (ProjectPlatformComboItem)jPlatformCombo.getSelectedItem();
        return selected != null
                ? selected.getPlatform()
                : properties.targetPlatformRef.tryGetValueWithoutFallback();
    }

    private ScriptPlatform getScriptPlatform(PropertyRefs properties) {
        if (jScriptPlatformInherit.isSelected()) {
            return null;
        }

        JavaPlatformComboItem selectedScriptPlatform = (JavaPlatformComboItem)jScriptPlatformCombo.getSelectedItem();
        if (selectedScriptPlatform != null) {
            PlatformSelectionMode selectionMode = toSelectionMode(properties.isSelectByVersion());

            JavaPlatform rawScriptPlatform = selectedScriptPlatform.getPlatform();
            return rawScriptPlatform != null
                    ? new ScriptPlatform(rawScriptPlatform, selectionMode)
                    : null;
        }
        else {
            return properties.scriptPlatformRef.tryGetValueWithoutFallback();
        }
    }

    private final class PropertyRefs implements ProfileEditor {
        private final PropertyReference<GradleLocationDef> gradleLocationRef;
        private final PropertyReference<ScriptPlatform> scriptPlatformRef;
        private final PropertyReference<Charset> sourceEncodingRef;
        private final PropertyReference<ProjectPlatform> targetPlatformRef;
        private final PropertyReference<String> sourceLevelRef;
        private final PropertyReference<UserInitScriptPath> userInitScriptPathRef;

        private final ProfileInfo profileInfo;

        public PropertyRefs(
                NbGradleProject ownerProject,
                ProfileInfo profileInfo,
                ActiveSettingsQuery settingsQuery) {
            this.gradleLocationRef = NbGradleCommonProperties.gradleLocation(settingsQuery);
            this.scriptPlatformRef = NbGradleCommonProperties.scriptPlatform(settingsQuery);
            this.sourceEncodingRef = NbGradleCommonProperties.sourceEncoding(settingsQuery);
            this.userInitScriptPathRef = NbGradleCommonProperties.userInitScriptPath(settingsQuery);
            this.targetPlatformRef = NbGradleCommonProperties.targetPlatform(ownerProject, settingsQuery);
            this.sourceLevelRef = NbGradleCommonProperties.sourceLevel(ownerProject, settingsQuery);

            this.profileInfo = profileInfo;
        }

        @Override
        public StoredSettings readFromSettings() {
            return new StoredSettingsImpl(this);
        }

        @Override
        public StoredSettings readFromGui() {
            return new StoredSettingsImpl(this, CommonProjectPropertiesPanel.this);
        }

        public boolean isSelectByVersion() {
            return profileInfo.isSharedProfile();
        }

        public boolean isCustomShouldPreferWrapper() {
            if (profileInfo.isGlobal()) {
                GradleLocationDef currentlyActiveLocation
                        = gradleLocationRef.tryGetValueWithoutFallback();
                return currentlyActiveLocation != null ? currentlyActiveLocation.isPreferWrapper() : false;
            }
            else {
                return false;
            }
        }
    }

    private final class StoredSettingsImpl implements StoredSettings {
        private final PropertyRefs properties;

        private final GradleLocationDef gradleLocation;
        private final ScriptPlatform scriptPlatform;
        private final Charset sourceEncoding;
        private final ProjectPlatform targetPlatform;
        private final String sourceLevel;
        private final UserInitScriptPath userInitScript;

        public StoredSettingsImpl(PropertyRefs properties) {
            this.properties = properties;
            this.gradleLocation = properties.gradleLocationRef.tryGetValueWithoutFallback();
            this.scriptPlatform = properties.scriptPlatformRef.tryGetValueWithoutFallback();
            this.sourceEncoding = properties.sourceEncodingRef.tryGetValueWithoutFallback();
            this.targetPlatform = properties.targetPlatformRef.tryGetValueWithoutFallback();
            this.sourceLevel = properties.sourceLevelRef.tryGetValueWithoutFallback();
            this.userInitScript = properties.userInitScriptPathRef.tryGetValueWithoutFallback();
        }

        public StoredSettingsImpl(PropertyRefs properties, CommonProjectPropertiesPanel panel) {
            this.properties = properties;

            gradleLocation = panel.getGradleLocation();
            scriptPlatform = panel.getScriptPlatform(properties);
            targetPlatform = panel.getTargetPlaform(properties);
            sourceEncoding = panel.getSelectedSourceEncoding(properties);
            sourceLevel = panel.getSourceLevel();
            userInitScript = panel.getUserInitScript();
        }

        @Override
        public void displaySettings() {
            CommonProjectPropertiesPanel.this.currentValues = properties;

            displayGradleLocation();
            displayScriptPlatform();
            displayTargetPlatform();
            displaySourceEncoding();
            displaySourceLevel();
            displayUserInitScript();
        }

        @Override
        public void saveSettings() {
            properties.gradleLocationRef.setValue(gradleLocation);
            properties.scriptPlatformRef.setValue(scriptPlatform);
            properties.sourceEncodingRef.setValue(sourceEncoding);
            properties.targetPlatformRef.setValue(targetPlatform);
            properties.sourceLevelRef.setValue(sourceLevel);
            properties.userInitScriptPathRef.setValue(userInitScript);
        }

        private void displayGradleLocation() {
            GradleLocationDef value = setInheritAndGetValue(
                    gradleLocation,
                    properties.gradleLocationRef,
                    jGradleHomeInherit);

            if (value != null) {
                selectGradleLocation(value);
            }
        }

        private void displayScriptPlatform() {
            displayScriptPlatform(properties.isSelectByVersion());
        }

        private void displayScriptPlatform(boolean selectByVersion) {
            ScriptPlatform value = setInheritAndGetValue(
                    scriptPlatform,
                    properties.scriptPlatformRef,
                    jScriptPlatformInherit);

            fillScriptPlatformCombo(selectByVersion);
            if (value != null) {
                PlatformSelectionMode selectionMode = toSelectionMode(selectByVersion);
                jScriptPlatformCombo.setSelectedItem(new JavaPlatformComboItem(value.getJavaPlatform(), selectionMode));
            }
        }

        private void displayTargetPlatform() {
            ProjectPlatform value = setInheritAndGetValue(
                    targetPlatform,
                    properties.targetPlatformRef,
                    jPlatformComboInherit);
            fillProjectPlatformCombo();
            if (value != null) {
                jPlatformCombo.setSelectedItem(new ProjectPlatformComboItem(value));
            }
        }

        private void displaySourceEncoding() {
            Charset value = setInheritAndGetValue(
                    sourceEncoding,
                    properties.sourceEncodingRef,
                    jSourceEncodingInherit);
            if (value != null) {
                jSourceEncoding.setSelectedItem(value);
            }
        }

        private void displaySourceLevel() {
            String value = setInheritAndGetValue(
                    sourceLevel,
                    properties.sourceLevelRef,
                    jSourceLevelComboInherit);
            if (value != null) {
                jSourceLevelCombo.setSelectedItem(value);
            }
        }

        private void displayUserInitScript() {
            UserInitScriptPath value = setInheritAndGetValue(
                    userInitScript,
                    properties.userInitScriptPathRef,
                    jUserInitScriptInherit);
            if (value != null) {
                jUserInitScript.setText(value.getRelPath().toString());
            }
        }
    }

    private static class JavaPlatformComboItem {
        private final JavaPlatform platform;
        private final PlatformSelectionMode selectionMode;
        private final String displayName;

        public JavaPlatformComboItem(JavaPlatform platform, PlatformSelectionMode selectionMode) {
            ExceptionHelper.checkNotNullArgument(platform, "platform");
            ExceptionHelper.checkNotNullArgument(selectionMode, "selectionMode");

            this.platform = platform;
            this.selectionMode = selectionMode;
            this.displayName = PlatformId.getDisplayNameOfPlatform(platform);
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        private SpecificationVersion tryGetVersion() {
            Specification specification = platform.getSpecification();
            return specification != null ? specification.getVersion() : null;
        }

        @Override
        public int hashCode() {
            int hash = 205;
            if (selectionMode == PlatformSelectionMode.BY_VERSION) {
                hash += Objects.hashCode(tryGetVersion());
            }
            else {
                hash += platform.hashCode();
            }
            hash = 41 * hash + Objects.hashCode(selectionMode);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final JavaPlatformComboItem other = (JavaPlatformComboItem)obj;
            if (other.selectionMode != this.selectionMode) {
                return false;
            }

            if (selectionMode == PlatformSelectionMode.BY_VERSION) {
                return Objects.equals(this.tryGetVersion(), other.tryGetVersion());
            }
            else {
                return Objects.equals(this.platform, other.platform);
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class ProjectPlatformComboItem {
        private final ProjectPlatform platform;
        private final String displayName;

        public ProjectPlatformComboItem(ProjectPlatform platform) {
            ExceptionHelper.checkNotNullArgument(platform, "platform");
            this.platform = platform;
            this.displayName = PlatformId.getDisplayNameOfPlatform(platform);
        }

        public ProjectPlatform getPlatform() {
            return platform;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + Objects.hashCode(platform.getName());
            hash = 41 * hash + Objects.hashCode(platform.getVersion());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ProjectPlatformComboItem other = (ProjectPlatformComboItem)obj;
            return Objects.equals(this.platform.getName(), other.platform.getName())
                    && Objects.equals(this.platform.getVersion(), other.platform.getVersion());
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "Convert2Diamond"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSourceLevelCombo = new javax.swing.JComboBox<>();
        jGradleHomeEdit = new javax.swing.JTextField();
        jGradleHomeInherit = new javax.swing.JCheckBox();
        jSourceEncodingInherit = new javax.swing.JCheckBox();
        jPlatformComboInherit = new javax.swing.JCheckBox();
        jScriptPlatformCombo = new javax.swing.JComboBox<>();
        jSourceLevelComboInherit = new javax.swing.JCheckBox();
        jScriptPlatformInherit = new javax.swing.JCheckBox();
        jPlatformPreferenceButton = new javax.swing.JButton();
        jPlatformCombo = new javax.swing.JComboBox<>();
        jGradleHomeCaption = new javax.swing.JLabel();
        jSourceEncodingCaption = new javax.swing.JLabel();
        jTargetPlatformCaption = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jSourceLevelCaption = new javax.swing.JLabel();
        jGradleHomeChangeButton = new javax.swing.JButton();
        jSourceEncoding = new javax.swing.JComboBox<>();
        jUserInitScriptCaption = new javax.swing.JLabel();
        jUserInitScript = new javax.swing.JTextField();
        jUserInitScriptInherit = new javax.swing.JCheckBox();
        jUserInitScriptBrowseButton = new javax.swing.JButton();

        jSourceLevelCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9" }));

        jGradleHomeEdit.setEditable(false);
        jGradleHomeEdit.setText(org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleHomeInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceEncodingInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformComboInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jPlatformComboInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelComboInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceLevelComboInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jScriptPlatformInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jScriptPlatformInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformPreferenceButton, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jPlatformPreferenceButton.text")); // NOI18N
        jPlatformPreferenceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPlatformPreferenceButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jGradleHomeCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceEncodingCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jTargetPlatformCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jTargetPlatformCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceLevelCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleHomeChangeButton, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeChangeButton.text")); // NOI18N
        jGradleHomeChangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jGradleHomeChangeButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jUserInitScriptCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jUserInitScriptCaption.text")); // NOI18N

        jUserInitScript.setText(org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jUserInitScript.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jUserInitScriptInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jUserInitScriptInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jUserInitScriptBrowseButton, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jUserInitScriptBrowseButton.text")); // NOI18N
        jUserInitScriptBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUserInitScriptBrowseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceEncodingCaption)
                            .addComponent(jGradleHomeCaption)
                            .addComponent(jTargetPlatformCaption)
                            .addComponent(jLabel1)
                            .addComponent(jSourceLevelCaption)
                            .addComponent(jUserInitScriptCaption))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPlatformPreferenceButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScriptPlatformCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPlatformCombo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jGradleHomeChangeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jGradleHomeEdit, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE))
                            .addComponent(jSourceEncoding, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jUserInitScript)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jUserInitScriptBrowseButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jSourceEncodingInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jGradleHomeInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jScriptPlatformInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPlatformComboInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jSourceLevelComboInherit, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addComponent(jUserInitScriptInherit, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSourceEncodingCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourceEncodingInherit)
                    .addComponent(jSourceEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleHomeCaption, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradleHomeEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jGradleHomeInherit)
                    .addComponent(jGradleHomeChangeButton))
                .addGap(12, 12, 12)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jScriptPlatformCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScriptPlatformInherit))
                .addGap(12, 12, 12)
                .addComponent(jTargetPlatformCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jPlatformCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPlatformComboInherit))
                .addGap(12, 12, 12)
                .addComponent(jSourceLevelCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSourceLevelComboInherit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jUserInitScriptCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jUserInitScript, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jUserInitScriptInherit)
                    .addComponent(jUserInitScriptBrowseButton))
                .addGap(18, 18, 18)
                .addComponent(jPlatformPreferenceButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jPlatformPreferenceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPlatformPreferenceButtonActionPerformed
        if (PlatformPriorityPanel.showDialog(this)) {
            if (currentValues != null) {
                refreshPlatformCombos(currentValues);
            }
        }
    }//GEN-LAST:event_jPlatformPreferenceButtonActionPerformed

    private void jGradleHomeChangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jGradleHomeChangeButtonActionPerformed
        if (currentValues == null) {
            return;
        }

        GradleLocationDef currentLocationDef = selectedGradleLocation != null
                ? selectedGradleLocation
                : currentValues.gradleLocationRef.getActiveValue();
        GradleLocationRef currentLocationRef = currentLocationDef != null ? currentLocationDef.getLocationRef() : null;

        GradleLocationRef newLocationRef = GradleLocationPanel.tryChooseLocation(this, getLocationResolver(), currentLocationRef);
        if (newLocationRef != null) {
            boolean preferWrapper = currentValues.isCustomShouldPreferWrapper();
            selectGradleLocation(new GradleLocationDef(newLocationRef, preferWrapper));
        }
    }//GEN-LAST:event_jGradleHomeChangeButtonActionPerformed

    private void jUserInitScriptBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jUserInitScriptBrowseButtonActionPerformed
        File settingsDir = project.currentModel().getValue().getSettingsDir().toFile();
        File initialDir = settingsDir;

        FileChooserBuilder dlgChooser = new FileChooserBuilder(
                LicenseHeaderPanel.class.getName() + "-" + initialDir.getName());
        dlgChooser.setDefaultWorkingDirectory(initialDir);
        // TODO: I18N
        dlgChooser.setFileFilter(new FileNameExtensionFilter("Gradle files", "gradle"));

        File f = dlgChooser.showOpenDialog();
        if (f == null || f.isDirectory()) {
            return;
        }

        File file = f.getAbsoluteFile();
        String relPath = NbFileUtils.tryMakeRelative(settingsDir, file);
        jUserInitScript.setText(relPath != null ? relPath : file.getPath());
    }//GEN-LAST:event_jUserInitScriptBrowseButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jGradleHomeCaption;
    private javax.swing.JButton jGradleHomeChangeButton;
    private javax.swing.JTextField jGradleHomeEdit;
    private javax.swing.JCheckBox jGradleHomeInherit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JComboBox<ProjectPlatformComboItem> jPlatformCombo;
    private javax.swing.JCheckBox jPlatformComboInherit;
    private javax.swing.JButton jPlatformPreferenceButton;
    private javax.swing.JComboBox<JavaPlatformComboItem> jScriptPlatformCombo;
    private javax.swing.JCheckBox jScriptPlatformInherit;
    private javax.swing.JComboBox<Charset> jSourceEncoding;
    private javax.swing.JLabel jSourceEncodingCaption;
    private javax.swing.JCheckBox jSourceEncodingInherit;
    private javax.swing.JLabel jSourceLevelCaption;
    private javax.swing.JComboBox<String> jSourceLevelCombo;
    private javax.swing.JCheckBox jSourceLevelComboInherit;
    private javax.swing.JLabel jTargetPlatformCaption;
    private javax.swing.JTextField jUserInitScript;
    private javax.swing.JButton jUserInitScriptBrowseButton;
    private javax.swing.JLabel jUserInitScriptCaption;
    private javax.swing.JCheckBox jUserInitScriptInherit;
    // End of variables declaration//GEN-END:variables
}
