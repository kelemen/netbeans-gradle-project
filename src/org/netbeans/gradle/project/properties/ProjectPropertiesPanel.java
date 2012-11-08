package org.netbeans.gradle.project.properties;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.modules.SpecificationVersion;

@SuppressWarnings("serial") // don't care
public class ProjectPropertiesPanel extends javax.swing.JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProjectPropertiesPanel.class.getName());
    private static final Collator STR_CMP = Collator.getInstance(Locale.getDefault());

    private ProfileItem currentlyShownProfile;
    private final Map<ProfileItem, ProjectProperties> storeForProperties;
    private final AtomicInteger profileChangeLock;
    private final NbGradleProject project;

    public ProjectPropertiesPanel(NbGradleProject project) {
        this.project = project;
        this.storeForProperties = new HashMap<ProfileItem, ProjectProperties>();
        this.currentlyShownProfile = null;

        initComponents();

        profileChangeLock = new AtomicInteger(0);

        setupListeners();
        fillPlatformCombo();

        fetchProfilesAndSelect();

        setEnableDisable();
    }

    private void sortProfiles(NbGradleConfiguration[] profileArray) {
        Arrays.sort(profileArray, new Comparator<NbGradleConfiguration>() {
            @Override
            public int compare(NbGradleConfiguration o1, NbGradleConfiguration o2) {
                return STR_CMP.compare(o1.getDisplayName(), o2.getDisplayName());
            }
        });

        // Make the default profile the first
        for (int i = 0; i < profileArray.length; i++) {
            if (NbGradleConfiguration.DEFAULT_CONFIG.equals(profileArray[i])) {
                for (int j = i; j > 0; j--) {
                    profileArray[j] = profileArray[j - 1];
                }
                profileArray[0] = NbGradleConfiguration.DEFAULT_CONFIG;
                break;
            }
        }
    }

    private void fillProfileCombo(Collection<NbGradleConfiguration> profiles) {
        List<ProfileItem> profileItems = new ArrayList<ProfileItem>(profiles.size() + 1);

        NbGradleConfiguration[] profileArray = profiles.toArray(new NbGradleConfiguration[0]);
        sortProfiles(profileArray);

        for (NbGradleConfiguration profile: profileArray) {
            profileItems.add(new ProfileItem(profile));
        }

        jProfileCombo.setModel(new DefaultComboBoxModel(profileItems.toArray(new ProfileItem[0])));
        jProfileCombo.setSelectedItem(new ProfileItem(project.getCurrentProfile()));
        loadSelectedProfile();
    }

    private void fetchProfilesAndSelect() {
        final AtomicReference<PanelLockRef> lockRef = new AtomicReference<PanelLockRef>(lockPanel());

        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Collection<NbGradleConfiguration> profiles =
                            project.getLookup().lookup(NbGradleConfigProvider.class).findAndUpdateConfigurations(false);

                    final PanelLockRef swingLock = lockRef.getAndSet(null);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                fillProfileCombo(profiles);
                            } finally {
                                swingLock.release();
                            }
                        }
                    });
                } finally {
                    PanelLockRef lock = lockRef.get();
                    if (lock != null) {
                        lock.release();
                    }
                }
            }
        });
    }

    private void initFromProperties(ProjectProperties properties) {
        JavaPlatform currentPlatform = properties.getPlatform().getValue();
        jPlatformCombo.setSelectedItem(new PlatformComboItem(currentPlatform));
        jPlatformComboInherit.setSelected(properties.getPlatform().isDefault());

        jSourceEncoding.setText(properties.getSourceEncoding().getValue().name());
        jSourceEncodingInherit.setSelected(properties.getSourceEncoding().isDefault());

        jSourceLevelCombo.setSelectedItem(properties.getSourceLevel().getValue());
        jSourceLevelComboInherit.setSelected(properties.getSourceLevel().isDefault());
    }

    private void sortProfileComboItems() {
        ComboBoxModel model = jProfileCombo.getModel();
        int itemCount = model.getSize();
        List<NbGradleConfiguration> configs = new ArrayList<NbGradleConfiguration>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            Object element = model.getElementAt(i);
            if (element instanceof ProfileItem) {
                configs.add(((ProfileItem)element).getConfig());
            }
        }

        NbGradleConfiguration[] configArray = configs.toArray(new NbGradleConfiguration[0]);
        sortProfiles(configArray);

        ProfileItem[] profiles = new ProfileItem[configArray.length];
        for (int i = 0; i < profiles.length; i++) {
            profiles[i] = new ProfileItem(configArray[i]);
        }
        jProfileCombo.setModel(new DefaultComboBoxModel(profiles));
    }

    private ProfileItem getSelectedProfile() {
        Object selected = jProfileCombo.getSelectedItem();
        if (selected instanceof ProfileItem) {
            return (ProfileItem)selected;
        }
        else {
            LOGGER.log(Level.SEVERE, "Profile combo contains item with unexpected type: {0}",
                    selected != null ? selected.getClass().getName() : "null");
            return null;
        }
    }

    private void loadSelectedProfile() {
        final ProfileItem selected = getSelectedProfile();
        if (selected == null) {
            return;
        }

        saveShownProfile();
        currentlyShownProfile = null;

        String profileName = selected.getProfileName();

        // If we already have a store for the properties then we should have
        // already edited it.
        ProjectProperties storedProperties = storeForProperties.get(selected);
        if (storedProperties != null) {
            currentlyShownProfile = selected;
            initFromProperties(storedProperties);
            return;
        }

        final PanelLockRef lock = lockPanel();
        project.getPropertiesForProfile(profileName, true, new PropertiesLoadListener() {
            @Override
            public void loadedProperties(final ProjectProperties properties) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            currentlyShownProfile = selected;
                            initFromProperties(properties);
                        } finally {
                            lock.release();
                        }
                    }
                });
            }
        });
    }

    private void setupListeners() {
        ChangeListener selectedChecked = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setEnableDisable();
            }
        };

        jPlatformComboInherit.getModel().addChangeListener(selectedChecked);
        jSourceEncodingInherit.getModel().addChangeListener(selectedChecked);
        jSourceLevelComboInherit.getModel().addChangeListener(selectedChecked);

        jProfileCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    loadSelectedProfile();
                }
            }
        });
    }

    private void fillPlatformCombo() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        List<PlatformComboItem> comboItems = new LinkedList<PlatformComboItem>();
        for (int i = 0; i < platforms.length; i++) {
            JavaPlatform platform = platforms[i];
            Specification specification = platform.getSpecification();
            if (specification != null && specification.getVersion() != null) {
                comboItems.add(new PlatformComboItem(platform));
            }
        }

        jPlatformCombo.setModel(new DefaultComboBoxModel(comboItems.toArray(new PlatformComboItem[0])));
    }

    private PanelLockRef lockPanel() {
        profileChangeLock.incrementAndGet();
        setEnableDisableThreadSafe();
        return new PanelLockRef();
    }

    private void setEnableDisableThreadSafe() {
        if (SwingUtilities.isEventDispatchThread()) {
            setEnableDisable();
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setEnableDisable();
                }
            });
        }
    }

    private void setEnableDisable() {
        boolean disableAll = profileChangeLock.get() > 0;

        for (Component subComponent: getComponents()) {
            if (subComponent != jProfileCombo) {
                subComponent.setEnabled(!disableAll);
            }
        }
        if (disableAll) {
            return;
        }

        jPlatformCombo.setEnabled(!jPlatformComboInherit.isSelected());
        jSourceEncoding.setEnabled(!jSourceEncodingInherit.isSelected());
        jSourceLevelCombo.setEnabled(!jSourceLevelComboInherit.isSelected());
    }

    private static <ValueType> PropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<ValueType>(value, defaultValue);
    }

    private static <ValueType> void copyProperty(MutableProperty<ValueType> src, MutableProperty<ValueType> dest) {
        dest.setValueFromSource(asConst(src.getValue(), src.isDefault()));
    }

    public void saveProperties() {
        saveShownProfile();
        for (Map.Entry<ProfileItem, ProjectProperties> entry: storeForProperties.entrySet()) {
            ProjectProperties src = entry.getValue();
            ProjectProperties dest = project.getPropertiesForProfile(entry.getKey().getProfileName(), false, null);

            copyProperty(src.getPlatform(), dest.getPlatform());
            copyProperty(src.getSourceEncoding(), dest.getSourceEncoding());
            copyProperty(src.getSourceLevel(), dest.getSourceLevel());
        }
    }

    private void saveShownProfile() {
        if (profileChangeLock.get() > 0) {
            // If profileChangeLock > 0 then we are still in the process of loading
            // the profile. This means, that it may not contain the loaded
            // values, so saving it would fill the properties with bogus values.
            //
            // In this case the controls should be disabled, so the user
            // did not have a chance to edit it anyway.
            return;
        }

        ProjectProperties properties = getStoreForShownProfile();
        if (properties == null) {
            return;
        }

        PlatformComboItem selected = (PlatformComboItem)jPlatformCombo.getSelectedItem();
        if (selected != null) {
            properties.getPlatform().setValueFromSource(asConst(selected.getPlatform(), jPlatformComboInherit.isSelected()));
        }

        String charsetName = jSourceEncoding.getText().trim();
        try {
            Charset newEncoding = Charset.forName(charsetName);
            properties.getSourceEncoding().setValueFromSource(asConst(newEncoding, jSourceEncodingInherit.isSelected()));
        } catch (IllegalCharsetNameException ex) {
            LOGGER.log(Level.INFO, "Illegal character set: " + charsetName, ex);
        } catch (UnsupportedCharsetException ex) {
            LOGGER.log(Level.INFO, "Unsupported character set: " + charsetName, ex);
        }

        String sourceLevel = (String)jSourceLevelCombo.getSelectedItem();
        if (sourceLevel != null) {
            properties.getSourceLevel().setValueFromSource(asConst(sourceLevel, jSourceLevelComboInherit.isSelected()));
        }
    }

    public JavaPlatform getSelectedPlatform() {
        PlatformComboItem selected = (PlatformComboItem)jPlatformCombo.getSelectedItem();
        return selected != null ? selected.getPlatform() : JavaPlatform.getDefault();
    }

    public Charset getSourceEncoding() {
        try {
            return Charset.forName(jSourceEncoding.getText().trim());
        } catch (IllegalCharsetNameException ex) {
            return MemProjectProperties.DEFAULT_SOURCE_ENCODING;
        } catch (UnsupportedCharsetException ex) {
            return MemProjectProperties.DEFAULT_SOURCE_ENCODING;
        }
    }

    private static class PlatformComboItem {
        private final JavaPlatform platform;

        public PlatformComboItem(JavaPlatform platform) {
            if (platform == null) throw new NullPointerException("platform");
            this.platform = platform;
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + (this.platform.getSpecification().getVersion().hashCode());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PlatformComboItem other = (PlatformComboItem)obj;
            SpecificationVersion thisVersion = this.platform.getSpecification().getVersion();
            SpecificationVersion otherVersion = other.platform.getSpecification().getVersion();
            return thisVersion.equals(otherVersion);
        }

        @Override
        public String toString() {
            return platform.getDisplayName();
        }
    }

    private void getSelectedProperties(boolean useInheritance, PropertiesLoadListener listener) {
        ProfileItem selectedProfile = getSelectedProfile();
        if (selectedProfile == null) {
            LOGGER.warning("No selected profile while clicking the manage tasks button.");
            return;
        }

        project.getPropertiesForProfile(selectedProfile.getProfileName(), useInheritance, listener);
    }

    private ProjectProperties getStoreForShownProfile() {
        ProfileItem profile = currentlyShownProfile;
        if (profile == null) {
            return null;
        }

        ProjectProperties properties = storeForProperties.get(profile);
        if (properties == null) {
            properties = new MemProjectProperties();
            storeForProperties.put(profile, properties);
        }
        return properties;
    }

    private static class ProfileItem {
        private static final ProfileItem DEFAULT_PROFILE = new ProfileItem(NbGradleConfiguration.DEFAULT_CONFIG);

        private final NbGradleConfiguration config;

        public ProfileItem(NbGradleConfiguration config) {
            if (config == null) throw new NullPointerException("config");
            this.config = config;
        }

        public String getProfileName() {
            return config.getProfileName();
        }

        public NbGradleConfiguration getConfig() {
            return config;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + (this.config != null ? this.config.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ProfileItem other = (ProfileItem)obj;
            if (this.config != other.config && (this.config == null || !this.config.equals(other.config)))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return config.getDisplayName();
        }
    }

    private class PanelLockRef {
        private final AtomicBoolean released = new AtomicBoolean(false);

        public void release() {
            if (released.compareAndSet(false, true)) {
                profileChangeLock.decrementAndGet();
                setEnableDisableThreadSafe();
            }
        }
    }

    private void displayManageTasksPanel(String profileName, ProjectProperties properties) {
        ManageTasksPanel panel = new ManageTasksPanel();
        panel.initSettings(properties);

        DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getManageTasksDlgTitle(profileName),
                true,
                new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);
        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.pack();
        dlg.setVisible(true);

        if (DialogDescriptor.OK_OPTION == dlgDescriptor.getValue()) {
            panel.saveTasks(properties);
        }
    }

    private void displayManageBuiltInTasksPanel(String profileName, ProjectProperties properties) {
        ManageBuiltInTasksPanel panel = new ManageBuiltInTasksPanel(properties);

        DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getManageBuiltInTasksDlgTitle(profileName),
                true,
                new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);
        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.pack();
        dlg.setVisible(true);

        if (DialogDescriptor.OK_OPTION == dlgDescriptor.getValue()) {
            panel.saveModifiedTasks();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSourceEncodingCaption = new javax.swing.JLabel();
        jSourceEncoding = new javax.swing.JTextField();
        jPlatformCaption = new javax.swing.JLabel();
        jPlatformCombo = new javax.swing.JComboBox();
        jSourceLevelCaption = new javax.swing.JLabel();
        jSourceLevelCombo = new javax.swing.JComboBox();
        jManageTasksButton = new javax.swing.JButton();
        jSourceEncodingInherit = new javax.swing.JCheckBox();
        jPlatformComboInherit = new javax.swing.JCheckBox();
        jSourceLevelComboInherit = new javax.swing.JCheckBox();
        jProfileCaption = new javax.swing.JLabel();
        jProfileCombo = new javax.swing.JComboBox();
        jAddProfileButton = new javax.swing.JButton();
        jRemoveProfileButton = new javax.swing.JButton();
        jManageBuiltInTasks = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceEncodingCaption.text")); // NOI18N

        jSourceEncoding.setText(org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceEncoding.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jPlatformCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceLevelCaption.text")); // NOI18N

        jSourceLevelCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.3", "1.4", "1.5", "1.6", "1.7", "1.8" }));

        org.openide.awt.Mnemonics.setLocalizedText(jManageTasksButton, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jManageTasksButton.text")); // NOI18N
        jManageTasksButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jManageTasksButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingInherit, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceEncodingInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformComboInherit, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jPlatformComboInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelComboInherit, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jSourceLevelComboInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jProfileCaption, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jProfileCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAddProfileButton, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jAddProfileButton.text")); // NOI18N
        jAddProfileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddProfileButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jRemoveProfileButton, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jRemoveProfileButton.text")); // NOI18N
        jRemoveProfileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRemoveProfileButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jManageBuiltInTasks, org.openide.util.NbBundle.getMessage(ProjectPropertiesPanel.class, "ProjectPropertiesPanel.jManageBuiltInTasks.text")); // NOI18N
        jManageBuiltInTasks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jManageBuiltInTasksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPlatformCombo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSourceEncoding, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceEncodingInherit, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPlatformComboInherit, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSourceLevelComboInherit, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jProfileCaption)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProfileCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jAddProfileButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jRemoveProfileButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceEncodingCaption)
                            .addComponent(jPlatformCaption)
                            .addComponent(jSourceLevelCaption)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jManageTasksButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jManageBuiltInTasks)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jProfileCaption)
                    .addComponent(jProfileCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jAddProfileButton)
                    .addComponent(jRemoveProfileButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addComponent(jSourceEncodingCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourceEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSourceEncodingInherit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPlatformCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jPlatformCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPlatformComboInherit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSourceLevelCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSourceLevelComboInherit))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jManageTasksButton)
                    .addComponent(jManageBuiltInTasks)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jManageTasksButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jManageTasksButtonActionPerformed
        ProfileItem selectedProfile = getSelectedProfile();
        final String profileName = selectedProfile != null
                ? selectedProfile.toString()
                : "";

        getSelectedProperties(false, new PropertiesLoadListener() {
            @Override
            public void loadedProperties(final ProjectProperties properties) {
                if (SwingUtilities.isEventDispatchThread()) {
                    displayManageTasksPanel(profileName, properties);
                    return;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        displayManageTasksPanel(profileName, properties);
                    }
                });
            }
        });
    }//GEN-LAST:event_jManageTasksButtonActionPerformed

    private void jRemoveProfileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRemoveProfileButtonActionPerformed
        ProfileItem profileItem = getSelectedProfile();
        if (profileItem == null) {
            return;
        }
        NbGradleConfiguration config = profileItem.getConfig();
        if (NbGradleConfiguration.DEFAULT_CONFIG.equals(config)) {
            // Cannot remove the default profile.
            return;
        }

        String yes = UIManager.getString("OptionPane.yesButtonText", Locale.getDefault());
        String no = UIManager.getString("OptionPane.noButtonText", Locale.getDefault());
        int confirmResult = JOptionPane.showOptionDialog(
                this,
                NbStrings.getConfirmRemoveProfile(config.getDisplayName()),
                "",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{yes, no},
                no);
        if (confirmResult != 0) {
            return;
        }

        // set this to null, so that we will not attempt to save the profile
        currentlyShownProfile = null;

        storeForProperties.remove(profileItem);
        jProfileCombo.removeItem(profileItem);
        jProfileCombo.setSelectedItem(new ProfileItem(NbGradleConfiguration.DEFAULT_CONFIG));

        project.getLookup().lookup(NbGradleConfigProvider.class).removeConfiguration(config);
    }//GEN-LAST:event_jRemoveProfileButtonActionPerformed

    private void jAddProfileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddProfileButtonActionPerformed
        final AddNewProfilePanel panel = new AddNewProfilePanel();
        panel.startValidation();

        final DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getAddNewProfileCaption(),
                true,
                new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);

        panel.addValidityChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                dlgDescriptor.setValid(panel.isValidProfileName());
            }
        });
        dlgDescriptor.setValid(panel.isValidProfileName());

        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.pack();
        dlg.setVisible(true);

        if (DialogDescriptor.OK_OPTION != dlgDescriptor.getValue()) {
            return;
        }

        String profileName = panel.getProfileName();
        if (profileName.isEmpty()) {
            return;
        }

        NbGradleConfiguration newConfig = new NbGradleConfiguration(profileName);
        ProfileItem newProfile = new ProfileItem(newConfig);

        project.getLookup().lookup(NbGradleConfigProvider.class).addConfiguration(newConfig);

        jProfileCombo.addItem(newProfile);
        sortProfileComboItems();
        jProfileCombo.setSelectedItem(newProfile);

    }//GEN-LAST:event_jAddProfileButtonActionPerformed

    private void jManageBuiltInTasksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jManageBuiltInTasksActionPerformed
        ProfileItem selectedProfile = getSelectedProfile();
        final String profileName = selectedProfile != null
                ? selectedProfile.toString()
                : "";

        getSelectedProperties(false, new PropertiesLoadListener() {
            @Override
            public void loadedProperties(final ProjectProperties properties) {
                if (SwingUtilities.isEventDispatchThread()) {
                    displayManageBuiltInTasksPanel(profileName, properties);
                    return;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        displayManageBuiltInTasksPanel(profileName, properties);
                    }
                });
            }
        });
    }//GEN-LAST:event_jManageBuiltInTasksActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddProfileButton;
    private javax.swing.JButton jManageBuiltInTasks;
    private javax.swing.JButton jManageTasksButton;
    private javax.swing.JLabel jPlatformCaption;
    private javax.swing.JComboBox jPlatformCombo;
    private javax.swing.JCheckBox jPlatformComboInherit;
    private javax.swing.JLabel jProfileCaption;
    private javax.swing.JComboBox jProfileCombo;
    private javax.swing.JButton jRemoveProfileButton;
    private javax.swing.JTextField jSourceEncoding;
    private javax.swing.JLabel jSourceEncodingCaption;
    private javax.swing.JCheckBox jSourceEncodingInherit;
    private javax.swing.JLabel jSourceLevelCaption;
    private javax.swing.JComboBox jSourceLevelCombo;
    private javax.swing.JCheckBox jSourceLevelComboInherit;
    // End of variables declaration//GEN-END:variables
}
