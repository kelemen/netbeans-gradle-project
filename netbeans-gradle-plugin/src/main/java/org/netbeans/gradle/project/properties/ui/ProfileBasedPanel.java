package org.netbeans.gradle.project.properties.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.api.config.ui.StoredSettings;
import org.netbeans.gradle.project.properties.AtomicIntProperty;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.GlassPanes;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

import static org.jtrim2.property.PropertyFactory.*;
import static org.jtrim2.property.swing.AutoDisplayState.*;
import static org.jtrim2.property.swing.SwingProperties.*;
import static org.netbeans.gradle.project.properties.NbProperties.*;

@SuppressWarnings("serial")
public class ProfileBasedPanel extends javax.swing.JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProfileBasedPanel.class.getName());

    private final NbGradleProject project;
    private final ProjectSettingsProvider.ExtensionSettings extensionSettings;
    private final ProfileEditorFactory snapshotCreator;
    private final AtomicIntProperty profileLoadCounter;
    private final Map<ProfileItem, Snapshot> snapshots;

    private final JLayer<?> customPanelLayer;
    private ProfileItem currentlyShownProfile;

    private ProfileBasedPanel(
            NbGradleProject project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            final JComponent customPanel,
            ProfileEditorFactory snapshotCreator) {

        Objects.requireNonNull(customPanel, "customPanel");

        this.project = Objects.requireNonNull(project, "project");
        this.extensionSettings = Objects.requireNonNull(extensionSettings, "extensionSettings");
        this.snapshotCreator = Objects.requireNonNull(snapshotCreator, "snapshotCreator");
        this.profileLoadCounter = new AtomicIntProperty(SwingExecutors.getStrictExecutor(false));
        this.currentlyShownProfile = null;
        this.snapshots = new HashMap<>();
        this.customPanelLayer = new JLayer<>(customPanel);

        initComponents();

        final JScrollPane customPanelScroller = new JScrollPane(customPanelLayer);
        final Runnable layerSizeUpdater = () -> {
            Dimension containerSize = customPanelScroller.getViewport().getExtentSize();
            int containerWidth = containerSize.width;
            int containerHeight = containerSize.height;

            Dimension contentSize = customPanel.getMinimumSize();
            int contentWidth = contentSize.width;
            int contentHeight = contentSize.height;

            int normWidth = Math.max(contentWidth, containerWidth);
            int normHeight = Math.max(contentHeight, containerHeight);
            customPanelLayer.setPreferredSize(new Dimension(normWidth, normHeight));
        };
        customPanelScroller.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layerSizeUpdater.run();
            }
        });
        layerSizeUpdater.run();

        jCustomPanelContainer.add(customPanelScroller);
        jProfileCombo.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadSelectedProfile();
            }
        });

        setupEnableDisable();
    }

    public static ProfileBasedPanel createPanel(
            NbGradleProject project,
            ProfileBasedSettingsPage settingsPage) {
        ProjectSettingsProvider settingsProvider = project.getProjectSettingsProvider();
        return createPanel(project, settingsProvider.getExtensionSettings(""), settingsPage);
    }

    public static ProfileBasedPanel createPanel(
            NbGradleProject project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            ProfileBasedSettingsPage settingsPage) {
        return createPanel(
                project,
                extensionSettings,
                settingsPage.getSettingsPanel(),
                settingsPage.getAsyncPanelInitializer(),
                settingsPage.getEditorFactory());
    }

    private static ProfileBasedPanel createPanel(
            NbGradleProject project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            JComponent customPanel,
            CancelableFunction<? extends Runnable> asyncPanelInitializer,
            ProfileEditorFactory snapshotCreator) {
        ProfileBasedPanel result = new ProfileBasedPanel(project, extensionSettings, customPanel, snapshotCreator);
        result.fetchProfilesAndSelect(asyncPanelInitializer);
        return result;
    }

    public void saveProperties() {
        readShownProfiles();
        for (Map.Entry<ProfileItem, Snapshot> entry: snapshots.entrySet()) {
            Snapshot values = entry.getValue();
            values.applyValues();
        }
    }

    private PropertySource<Boolean> selectedRemovable() {
        return convert(comboBoxSelection(jProfileCombo), input -> input != null && input.isRemovable());
    }

    private void setupEnableDisable() {
        addSwingStateListener(lessThanOrEqual(profileLoadCounter, 0),
                glassPaneSwitcher(customPanelLayer, GlassPanes.delayedLoadingPanel(NbStrings.getLoading())));

        addSwingStateListener(selectedRemovable(), componentDisabler(jRemoveProfileButton));
    }

    private List<ProfileItem> getProfileItems() {
        Collection<NbGradleConfiguration> projectConfigs = project.getConfigProvider().getConfigurations();
        List<ProfileItem> result = new ArrayList<>(projectConfigs.size() + 1);
        for (NbGradleConfiguration config: projectConfigs) {
            result.add(new ProfileItem(config.getProfileDef()));
        }
        result.add(ProfileItem.GLOBAL_DEFAULT);
        Collections.sort(result, ProfileItem.ALPHABETICAL_ORDER);
        return result;
    }

    private CancelableFunction<Runnable> initProfileComboTask() {
        return (CancellationToken cancelToken) -> {
            List<ProfileItem> profileItems = getProfileItems();
            return fillProfileComboTask(profileItems);
        };
    }

    private Runnable fillProfileComboTask(List<ProfileItem> profileItems) {
        return () -> fillProfileCombo(profileItems);
    }

    private Runnable toReleaseTask(final PanelLockRef taskLock, final Runnable task) {
        return () -> {
            try {
                task.run();
            } finally {
                taskLock.release();
            }
        };
    }

    private void fetchProfilesAndSelect(CancelableFunction<? extends Runnable> asyncPanelInitializer) {
        final List<CancelableFunction<? extends Runnable>> initTasks = Arrays.<CancelableFunction<? extends Runnable>>asList(
                asyncPanelInitializer,
                initProfileComboTask());

        final PanelLockRef mainLockRef = lockPanel();
        NbTaskExecutors.DEFAULT_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            for (CancelableFunction<? extends Runnable> initTask: initTasks) {
                Runnable uiUpdateTask = initTask.execute(cancelToken);
                if (uiUpdateTask != null) {
                    PanelLockRef taskLock = lockPanel();
                    SwingUtilities.invokeLater(toReleaseTask(taskLock, uiUpdateTask));
                }
            }
        }).whenComplete((result, error) -> {
            mainLockRef.release();
        }).exceptionally(AsyncTasks::expectNoError);
    }

    private void fillProfileCombo(Collection<ProfileItem> profileItems) {
        jProfileCombo.setModel(new DefaultComboBoxModel<>(profileItems.toArray(new ProfileItem[profileItems.size()])));
        jProfileCombo.setSelectedItem(new ProfileItem(project.getConfigProvider().getActiveConfiguration().getProfileDef()));
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

        Snapshot values = getValuesForShownProfile();
        if (values != null) {
            values.readFromGui();
        }
    }

    private Snapshot getValuesForShownProfile() {
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
        Snapshot storedProperties = snapshots.get(selected);
        if (storedProperties != null) {
            currentlyShownProfile = selected;
            storedProperties.displayValues();
            return;
        }

        final ProfileKey profileKey = selected.getProfileKey();
        final PanelLockRef lock = lockPanel();
        extensionSettings.loadSettingsForProfile(Cancellation.UNCANCELABLE_TOKEN, profileKey, (ActiveSettingsQuery settings) -> {
            String displayName = selected.toString();
            ProfileInfo profileInfo = new ProfileInfo(profileKey, displayName);
            ProfileEditor editor = snapshotCreator.startEditingProfile(profileInfo, settings);
            final Snapshot snapshot = new Snapshot(editor);
            snapshots.put(selected, snapshot);

            SwingUtilities.invokeLater(() -> {
                try {
                    currentlyShownProfile = selected;
                    snapshot.displayValues();
                } finally {
                    lock.release();
                }
            });
        });
    }

    private void sortProfileComboItems() {
        ComboBoxModel<ProfileItem> model = jProfileCombo.getModel();
        int itemCount = model.getSize();
        List<ProfileItem> configs = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            Object element = model.getElementAt(i);
            if (element instanceof ProfileItem) {
                configs.add((ProfileItem)element);
            }
        }

        Collections.sort(configs, ProfileItem.ALPHABETICAL_ORDER);

        jProfileCombo.setModel(new DefaultComboBoxModel<>(configs.toArray(new ProfileItem[0])));
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
        private static final ProfileItem DEFAULT = new ProfileItem(null);

        private static final ProfileItem GLOBAL_DEFAULT
                = new ProfileItem(new ProfileDef(ProfileKey.GLOBAL_PROFILE, NbStrings.getGlobalProfileName()));

        private static final Comparator<ProfileItem> ALPHABETICAL_ORDER = (ProfileItem o1, ProfileItem o2) -> {
            if (GLOBAL_DEFAULT.equals(o1)) {
                return GLOBAL_DEFAULT.equals(o2) ? 0 : -1;
            }
            if (GLOBAL_DEFAULT.equals(o2)) {
                return GLOBAL_DEFAULT.equals(o1) ? 0 : 1;
            }

            if (DEFAULT.equals(o1)) {
                return DEFAULT.equals(o2) ? 0 : -1;
            }
            if (DEFAULT.equals(o2)) {
                return DEFAULT.equals(o1) ? 0 : 1;
            }

            return StringUtils.STR_CMP.compare(o1.toString(), o2.toString());
        };

        private final ProfileDef profileDef;

        public ProfileItem(ProfileDef profileDef) {
            this.profileDef = profileDef;
        }

        public boolean isRemovable() {
            return profileDef != null && !ProfileKey.GLOBAL_PROFILE.equals(profileDef.getProfileKey());
        }

        public ProfileKey getProfileKey() {
            return profileDef != null ? profileDef.getProfileKey() : null;
        }

        public ProfileDef getProfileDef() {
            return profileDef;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + Objects.hashCode(profileDef);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ProfileItem other = (ProfileItem)obj;
            return Objects.equals(this.profileDef, other.profileDef);
        }

        @Override
        public String toString() {
            return profileDef != null
                    ? profileDef.getDisplayName()
                    : NbStrings.getDefaultProfileName();
        }
    }

    private static final class Snapshot {
        private final ProfileEditor editor;
        private final AtomicReference<StoredSettings> currentSettingsRef;

        public Snapshot(ProfileEditor editor) {
            this.editor = editor;
            this.currentSettingsRef = new AtomicReference<>(editor.readFromSettings());
        }

        private void displayValues() {
            StoredSettings settings = currentSettingsRef.get();
            settings.displaySettings();
        }

        private void readFromGui() {
            currentSettingsRef.set(editor.readFromGui());
        }

        private void applyValues() {
            StoredSettings settings = currentSettingsRef.get();
            settings.saveSettings();
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
        jProfileCombo = new javax.swing.JComboBox<>();
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
        validProfileName.addChangeListener(() -> {
            dlgDescriptor.setValid(validProfileName.getValue());
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
        ProfileItem newProfile = new ProfileItem(profileDef);

        project.getConfigProvider().addConfiguration(newConfig);

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
        if (profileItem == null || !profileItem.isRemovable()) {
            return;
        }

        NotifyDescriptor d = new NotifyDescriptor.Confirmation(
                NbStrings.getConfirmRemoveProfile(profileItem.toString()),
                NotifyDescriptor.YES_NO_OPTION,
                NotifyDescriptor.WARNING_MESSAGE);
        if(DialogDisplayer.getDefault().notify(d) == NotifyDescriptor.NO_OPTION) {
            return;
        }

        // set this to null, so that we will not attempt to save the profile
        currentlyShownProfile = null;

        snapshots.remove(profileItem);
        jProfileCombo.removeItem(profileItem);
        jProfileCombo.setSelectedItem(ProfileItem.DEFAULT);

        NbGradleConfiguration config = new NbGradleConfiguration(profileItem.profileDef);
        project.getConfigProvider().removeConfiguration(config);
    }//GEN-LAST:event_jRemoveProfileButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddProfileButton;
    private javax.swing.JPanel jCustomPanelContainer;
    private javax.swing.JLabel jProfileCaption;
    private javax.swing.JComboBox<ProfileItem> jProfileCombo;
    private javax.swing.JButton jRemoveProfileButton;
    // End of variables declaration//GEN-END:variables
}
