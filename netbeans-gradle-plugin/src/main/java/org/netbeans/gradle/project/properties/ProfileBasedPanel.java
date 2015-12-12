package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

import static org.jtrim.property.BoolProperties.isNull;
import static org.jtrim.property.BoolProperties.not;
import static org.jtrim.property.PropertyFactory.convert;
import static org.jtrim.property.swing.AutoDisplayState.*;
import static org.jtrim.property.swing.SwingProperties.comboBoxSelection;
import static org.netbeans.gradle.project.properties.NbProperties.lessThanOrEqual;

@SuppressWarnings("serial")
public class ProfileBasedPanel extends javax.swing.JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProfileBasedPanel.class.getName());

    private final NbGradleProject project;
    private final ProjectSettingsProvider.ExtensionSettings extensionSettings;
    private final ProfileValuesEditorFactory snapshotCreator;
    private final AtomicIntProperty profileLoadCounter;
    private final Map<ProfileItem, ProfileValuesEditor> snapshots;

    private final JLayer<?> customPanelLayer;
    private ProfileItem currentlyShownProfile;

    private ProfileBasedPanel(
            NbGradleProject project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            final JComponent customPanel,
            ProfileValuesEditorFactory snapshotCreator) {

        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(extensionSettings, "extensionSettings");
        ExceptionHelper.checkNotNullArgument(customPanel, "customPanel");
        ExceptionHelper.checkNotNullArgument(snapshotCreator, "snapshotCreator");

        this.project = project;
        this.extensionSettings = extensionSettings;
        this.snapshotCreator = snapshotCreator;
        this.profileLoadCounter = new AtomicIntProperty(SwingTaskExecutor.getStrictExecutor(false));
        this.currentlyShownProfile = null;
        this.snapshots = new HashMap<>();
        this.customPanelLayer = new JLayer<>(customPanel);

        initComponents();

        final JScrollPane customPanelScroller = new JScrollPane(customPanelLayer);
        final Runnable layerSizeUpdater = new Runnable() {
            @Override
            public void run() {
                Dimension containerSize = customPanelScroller.getViewport().getExtentSize();
                int containerWidth = containerSize.width;
                int containerHeight = containerSize.height;

                Dimension contentSize = customPanel.getMinimumSize();
                int contentWidth = contentSize.width;
                int contentHeight = contentSize.height;

                int width = Math.max(contentWidth, containerWidth);
                int height = Math.max(contentHeight, containerHeight);

                customPanelLayer.setPreferredSize(new Dimension(width, height));
            }
        };
        customPanelScroller.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layerSizeUpdater.run();
            }
        });
        layerSizeUpdater.run();

        jCustomPanelContainer.add(customPanelScroller);
        jProfileCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    loadSelectedProfile();
                }
            }
        });

        setupEnableDisable();
    }

    public static ProfileBasedPanel createPanel(
            Project project,
            JComponent customPanel,
            ProfileValuesEditorFactory snapshotCreator) {

        final NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            throw new IllegalArgumentException("Not a Gradle project: " + project.getProjectDirectory());
        }
        ProjectSettingsProvider.ExtensionSettings extensionSettings = new ProjectSettingsProvider.ExtensionSettings() {
            @Override
            public ActiveSettingsQuery getActiveSettings() {
                return gradleProject.getActiveSettingsQuery();
            }

            @Override
            public ActiveSettingsQuery loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile) {
                return gradleProject.loadActiveSettingsForProfile(profile);
            }

            @Override
            public void loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile, ActiveSettingsQueryListener settingsQueryListener) {
                gradleProject.loadActiveSettingsForProfile(profile, settingsQueryListener);
            }
        };
        return createPanel(project, extensionSettings, customPanel, snapshotCreator);
    }

    public static ProfileBasedPanel createPanel(
            Project project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            JComponent customPanel,
            ProfileValuesEditorFactory snapshotCreator) {
        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            throw new IllegalArgumentException("Not a Gradle project: " + project.getProjectDirectory());
        }

        return createPanel(gradleProject, extensionSettings, customPanel, snapshotCreator);
    }

    private static ProfileBasedPanel createPanel(
            NbGradleProject project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            JComponent customPanel,
            ProfileValuesEditorFactory snapshotCreator) {
        ProfileBasedPanel result = new ProfileBasedPanel(project, extensionSettings, customPanel, snapshotCreator);
        result.fetchProfilesAndSelect();
        return result;
    }

    public void saveProperties() {
        readShownProfiles();
        for (Map.Entry<ProfileItem, ProfileValuesEditor> entry: snapshots.entrySet()) {
            ProfileValuesEditor values = entry.getValue();
            values.applyValues();
        }
    }

    private PropertySource<ProfileKey> selectedProfileKey() {
        return convert(comboBoxSelection(jProfileCombo), new ValueConverter<ProfileItem, ProfileKey>() {
            @Override
            public ProfileKey convert(ProfileItem input) {
                return input != null ? input.getProfileKey() : null;
            }
        });
    }

    private void setupEnableDisable() {
        addSwingStateListener(lessThanOrEqual(profileLoadCounter, 0),
                glassPaneSwitcher(customPanelLayer, GlassPanes.delayedLoadingPanel(NbStrings.getLoading())));

        addSwingStateListener(not(isNull(selectedProfileKey())),
                componentDisabler(jRemoveProfileButton));
    }

    private void fetchProfilesAndSelect() {
        final AtomicReference<PanelLockRef> lockRef = new AtomicReference<>(lockPanel());

        NbTaskExecutors.DEFAULT_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                final Collection<NbGradleConfiguration> profiles = project
                        .getLookup()
                        .lookup(NbGradleSingleProjectConfigProvider.class)
                        .getConfigurations();

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
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                NbTaskExecutors.defaultCleanup(canceled, error);
                PanelLockRef lock = lockRef.get();
                if (lock != null) {
                    lock.release();
                }
            }
        });
    }

    private void fillProfileCombo(Collection<NbGradleConfiguration> profiles) {
        List<ProfileItem> profileItems = new ArrayList<>(profiles.size() + 1);

        NbGradleConfiguration[] profileArray = profiles.toArray(new NbGradleConfiguration[0]);
        NbGradleConfiguration.sortProfiles(profileArray);

        for (NbGradleConfiguration profile: profileArray) {
            profileItems.add(new ProfileItem(profile));
        }

        jProfileCombo.setModel(new DefaultComboBoxModel<>(profileItems.toArray(new ProfileItem[profileItems.size()])));
        jProfileCombo.setSelectedItem(new ProfileItem(project.getCurrentProfile()));
        loadSelectedProfile();
    }

    private void readShownProfiles() {
        if (profileLoadCounter.getIntValue() > 0) {
            // If profileLoadCounter > 0 then we are still in the process of loading
            // the profile. This means, that it may not contain the loaded
            // values, so saving it would fill the properties with bogus values.
            //
            // In this case the controls should be disabled, so the user
            // did not have a chance to edit it anyway.
            return;
        }

        ProfileValuesEditor values = getValuesForShownProfile();
        if (values != null) {
            values.readFromGui();
        }
    }

    private ProfileValuesEditor getValuesForShownProfile() {
        ProfileItem profile = currentlyShownProfile;
        if (profile == null) {
            return null;
        }

        return snapshots.get(profile);
    }

    private void loadSelectedProfile() {
        final ProfileItem selected = getSelectedProfile();
        if (selected == null) {
            return;
        }

        readShownProfiles();
        currentlyShownProfile = null;

        // If we already have a store for the properties then we should have
        // already edited it.
        ProfileValuesEditor storedProperties = snapshots.get(selected);
        if (storedProperties != null) {
            currentlyShownProfile = selected;
            storedProperties.displayValues();
            return;
        }

        final PanelLockRef lock = lockPanel();
        extensionSettings.loadSettingsForProfile(Cancellation.UNCANCELABLE_TOKEN, selected.getProfileKey(), new ActiveSettingsQueryListener() {
            @Override
            public void onLoad(final ActiveSettingsQuery settings) {
                String displayName = selected.config.getDisplayName();
                final ProfileValuesEditor snapshot = snapshotCreator.startEditingProfile(displayName, settings);
                snapshots.put(selected, snapshot);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            currentlyShownProfile = selected;
                            snapshot.displayValues();
                        } finally {
                            lock.release();
                        }
                    }
                });
            }
        });
    }

    private void sortProfileComboItems() {
        ComboBoxModel<ProfileItem> model = jProfileCombo.getModel();
        int itemCount = model.getSize();
        List<NbGradleConfiguration> configs = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            Object element = model.getElementAt(i);
            if (element instanceof ProfileItem) {
                configs.add(((ProfileItem)element).getConfig());
            }
        }

        NbGradleConfiguration[] configArray = configs.toArray(new NbGradleConfiguration[0]);
        NbGradleConfiguration.sortProfiles(configArray);

        ProfileItem[] profiles = new ProfileItem[configArray.length];
        for (int i = 0; i < profiles.length; i++) {
            profiles[i] = new ProfileItem(configArray[i]);
        }
        jProfileCombo.setModel(new DefaultComboBoxModel<>(profiles));
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

    private PanelLockRef lockPanel() {
        profileLoadCounter.getAndIncrement();
        return new PanelLockRef();
    }

    private final class PanelLockRef {
        private final AtomicBoolean released = new AtomicBoolean(false);

        public void release() {
            if (released.compareAndSet(false, true)) {
                profileLoadCounter.getAndDecrement();
            }
        }
    }

    private static final class ProfileItem {
        private static final ProfileItem DEFAULT
                = new ProfileItem(NbGradleConfiguration.DEFAULT_CONFIG);

        private final NbGradleConfiguration config;

        public ProfileItem(NbGradleConfiguration config) {
            ExceptionHelper.checkNotNullArgument(config, "config");
            this.config = config;
        }

        public ProfileKey getProfileKey() {
            return config.getProfileKey();
        }

        public ProfileDef getProfileDef() {
            return config.getProfileDef();
        }

        public NbGradleConfiguration getConfig() {
            return config;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + Objects.hashCode(config);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ProfileItem other = (ProfileItem)obj;
            return Objects.equals(this.config, other.config);
        }

        @Override
        public String toString() {
            return config.getDisplayName();
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

        jProfileCaption = new javax.swing.JLabel();
        jProfileCombo = new javax.swing.JComboBox<ProfileItem>();
        jAddProfileButton = new javax.swing.JButton();
        jRemoveProfileButton = new javax.swing.JButton();
        jCustomPanelContainer = new javax.swing.JPanel();

        org.openide.awt.Mnemonics.setLocalizedText(jProfileCaption, org.openide.util.NbBundle.getMessage(ProfileBasedPanel.class, "ProfileBasedPanel.jProfileCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAddProfileButton, org.openide.util.NbBundle.getMessage(ProfileBasedPanel.class, "ProfileBasedPanel.jAddProfileButton.text")); // NOI18N
        jAddProfileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddProfileButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jRemoveProfileButton, org.openide.util.NbBundle.getMessage(ProfileBasedPanel.class, "ProfileBasedPanel.jRemoveProfileButton.text")); // NOI18N
        jRemoveProfileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRemoveProfileButtonActionPerformed(evt);
            }
        });

        jCustomPanelContainer.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jProfileCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProfileCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jAddProfileButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 69, Short.MAX_VALUE)
                .addComponent(jRemoveProfileButton)
                .addContainerGap())
            .addComponent(jCustomPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCustomPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

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

        final PropertySource<Boolean> validProfileName = panel.validProfileName();
        validProfileName.addChangeListener(new Runnable() {
            @Override
            public void run() {
                dlgDescriptor.setValid(validProfileName.getValue());
            }
        });
        dlgDescriptor.setValid(validProfileName.getValue());

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
        ProfileDef profileDef = SettingsFiles.getStandardProfileDef(profileName);
        NbGradleConfiguration newConfig = new NbGradleConfiguration(profileDef);
        ProfileItem newProfile = new ProfileItem(newConfig);

        project.getLookup().lookup(NbGradleSingleProjectConfigProvider.class).addConfiguration(newConfig);

        boolean hasItem = false;
        int itemCount = jProfileCombo.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (newProfile.equals(jProfileCombo.getItemAt(i))) {
                hasItem = true;
                break;
            }
        }

        if (!hasItem) {
            jProfileCombo.addItem(newProfile);
            sortProfileComboItems();
        }
        jProfileCombo.setSelectedItem(newProfile);
    }//GEN-LAST:event_jAddProfileButtonActionPerformed

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

        snapshots.remove(profileItem);
        jProfileCombo.removeItem(profileItem);
        jProfileCombo.setSelectedItem(ProfileItem.DEFAULT);

        project.getLookup().lookup(NbGradleSingleProjectConfigProvider.class).removeConfiguration(config);
    }//GEN-LAST:event_jRemoveProfileButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddProfileButton;
    private javax.swing.JPanel jCustomPanelContainer;
    private javax.swing.JLabel jProfileCaption;
    private javax.swing.JComboBox<ProfileItem> jProfileCombo;
    private javax.swing.JButton jRemoveProfileButton;
    // End of variables declaration//GEN-END:variables
}
