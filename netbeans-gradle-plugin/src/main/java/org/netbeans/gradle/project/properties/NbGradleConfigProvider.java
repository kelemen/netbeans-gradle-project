package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.util.SerializationUtils2;
import org.netbeans.spi.project.ProjectConfigurationProvider;

public final class NbGradleConfigProvider {
    private static final Logger LOGGER = Logger.getLogger(NbGradleConfigProvider.class.getName());

    // Must be FIFO executor
    // Warning: May not wait for multiProfileProperties to be loaded on this executor.
    private static final TaskExecutor PROFILE_APPLIER_EXECUTOR
            = NbTaskExecutors.newExecutor("Profile-applier", 1);

    private static final String LAST_PROFILE_FILE = "last-profile";

    private static final Lock CONFIG_PROVIDERS_LOCK = new ReentrantLock();
    private static final Map<Path, NbGradleConfigProvider> CONFIG_PROVIDERS
            = new WeakValueHashMap<>();

    private final Path rootDirectory;
    private final PropertyChangeSupport changeSupport;
    private final ChangeListenerManager activeConfigChangeListeners;
    private final AtomicReference<List<NbGradleConfiguration>> configs;
    private final AtomicReference<NbGradleConfiguration> activeConfig;
    private final AtomicBoolean hasBeenUsed;
    private volatile boolean hasActiveBeenSet;

    private final MultiProfileProperties multiProfileProperties;
    private final ProfileSettingsContainer settingsContainer;

    private final UpdateTaskExecutor profileApplierExecutor;

    private final MonitorableTaskExecutor profileIOExecutor;

    private NbGradleConfigProvider(Path rootDirectory) {
        ExceptionHelper.checkNotNullArgument(rootDirectory, "rootDirectory");

        this.rootDirectory = rootDirectory;
        this.hasBeenUsed = new AtomicBoolean(false);
        this.changeSupport = new PropertyChangeSupport(this);
        this.activeConfigChangeListeners = new GenericChangeListenerManager();
        this.activeConfig = new AtomicReference<>(NbGradleConfiguration.DEFAULT_CONFIG);
        this.configs = new AtomicReference<>(
                Collections.singletonList(NbGradleConfiguration.DEFAULT_CONFIG));
        this.hasActiveBeenSet = false;
        this.multiProfileProperties = new MultiProfileProperties();
        this.settingsContainer = ProfileSettingsContainer.getDefault();
        this.profileApplierExecutor = new GenericUpdateTaskExecutor(PROFILE_APPLIER_EXECUTOR);
        this.profileIOExecutor = NbTaskExecutors.newDefaultFifoExecutor();
    }

    public static NbGradleConfigProvider getConfigProvider(NbGradleProject project) {
        Path rootDir = SettingsFiles.getRootDirectory(project);

        CONFIG_PROVIDERS_LOCK.lock();
        try {
            NbGradleConfigProvider result = CONFIG_PROVIDERS.get(rootDir);
            if (result == null) {
                result = new NbGradleConfigProvider(rootDir);
                result.updateByKey(result.activeConfig.get().getProfileKey());
                CONFIG_PROVIDERS.put(rootDir, result);
            }
            return result;
        } finally {
            CONFIG_PROVIDERS_LOCK.unlock();
        }
    }

    public ActiveSettingsQueryEx getActiveSettingsQuery() {
        return multiProfileProperties;
    }

    public ProfileSettingsContainer getProfileSettingsContainer() {
        return settingsContainer;
    }

    private void removeFromConfig(NbGradleConfiguration config) {
        List<NbGradleConfiguration> currentList;
        List<NbGradleConfiguration> newList;

        do {
            currentList = configs.get();
            newList = new ArrayList<>(currentList);
            newList.remove(config);
            newList = Collections.unmodifiableList(newList);
        } while (!configs.compareAndSet(currentList, newList));
    }

    private void addToConfig(Collection<NbGradleConfiguration> toAdd) {
        List<NbGradleConfiguration> currentList;
        List<NbGradleConfiguration> newList;

        do {
            currentList = configs.get();
            Set<NbGradleConfiguration> configSet = new HashSet<>(currentList);
            configSet.addAll(toAdd);

            newList = new ArrayList<>(configSet);
            NbGradleConfiguration.sortProfiles(newList);

            newList = Collections.unmodifiableList(newList);
        } while (!configs.compareAndSet(currentList, newList));
    }

    public void removeConfiguration(final NbGradleConfiguration config) {
        if (NbGradleConfiguration.DEFAULT_CONFIG.equals(config)) {
            LOGGER.warning("Cannot remove the default configuration");
            return;
        }

        profileIOExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                removeFromConfig(config);
                Path profileFile = SettingsFiles.getFilesForProfile(rootDirectory, config.getProfileDef())[0];
                if (!Files.deleteIfExists(profileFile)) {
                    LOGGER.log(Level.INFO, "Profile was deleted but no profile file was found: {0}", profileFile);
                }

                fireConfigurationListChange();

            }
        }, null);
    }

    public void addConfiguration(final NbGradleConfiguration config) {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                addToConfig(Collections.singleton(config));
                fireConfigurationListChange();
            }
        });
    }

    private void executeOnEdt(Runnable task) {
        assert task != null;

        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            SwingUtilities.invokeLater(task);
        }
    }

    public Collection<NbGradleConfiguration> findAndUpdateConfigurations(boolean mayRemove) {
        Collection<ProfileDef> profileDefs = SettingsFiles.getAvailableProfiles(rootDirectory);
        List<NbGradleConfiguration> currentConfigs
                = new ArrayList<>(profileDefs.size() + 1);

        currentConfigs.add(NbGradleConfiguration.DEFAULT_CONFIG);
        for (ProfileDef profileDef: profileDefs) {
            currentConfigs.add(new NbGradleConfiguration(profileDef));
        }

        if (mayRemove) {
            configs.set(Collections.unmodifiableList(currentConfigs));
        }
        else {
            addToConfig(currentConfigs);
        }

        // Only switch automatically for custom profiles because our wrapper
        // might actually allow other profiles.
        NbGradleConfiguration config = activeConfig.get();
        if (config.getProfileGroup() == null && !configs.get().contains(config)) {
            setActiveConfiguration(NbGradleConfiguration.DEFAULT_CONFIG);
        }

        fireConfigurationListChange();
        return configs.get();
    }

    private Path getLastProfileFile() {
        return SettingsFiles.getPrivateSettingsDir(rootDirectory).resolve(LAST_PROFILE_FILE);
    }

    private void readAndUpdateDefaultProfile() {
        Path lastProfileFile = getLastProfileFile();
        if (!Files.isRegularFile(lastProfileFile)) {
            return;
        }

        SavedProfileDef savedDef;
        try {
            savedDef = (SavedProfileDef)SerializationUtils2.deserializeFile(lastProfileFile);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to read last profile.", ex);
            return;
        }

        NbGradleConfiguration lastConfig = savedDef.findSameConfig(configs.get());
        if (lastConfig == null) {
            return;
        }

        // FIXME: This is not actually thread-safe. If the user sets the
        // configuration concurrently with this call, we may overwrite the user's
        // choice. However, this is very unlikely and even if it happens it is
        // just a minor inconvenience.
        if (!hasActiveBeenSet) {
            setActiveConfiguration(lastConfig);
        }
    }

    private void saveActiveProfileNow() throws IOException {
        assert profileIOExecutor.isExecutingInThis();

        Path lastProfileFile = getLastProfileFile();

        NbGradleConfiguration config = activeConfig.get();
        if (config != null) {
            ProfileDef profileDef = config.getProfileDef();
            if (profileDef == null) {
                if (!Files.deleteIfExists(lastProfileFile)) {
                    LOGGER.log(Level.FINE, "Last profile file could not be deleted: {0}", lastProfileFile);
                }
                return;
            }

            SavedProfileDef savedDef = new SavedProfileDef(profileDef);

            try {
                Path parentFile = lastProfileFile.getParent();
                if (parentFile != null) {
                    Files.createDirectories(parentFile);
                }

                SerializationUtils2.serializeToFile(lastProfileFile, savedDef);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Failed to read last profile.", ex);
            }
        }
    }

    private void saveActiveProfile() {
        profileIOExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                saveActiveProfileNow();
            }
        }, null);
    }

    private void ensureLoadedAsynchronously() {
        if (hasBeenUsed.compareAndSet(false, true)) {
            profileIOExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    findAndUpdateConfigurations(false);
                    readAndUpdateDefaultProfile();
                }
            }, null);
        }
    }

    private void fireActiveConfigurationListChange(final NbGradleConfiguration prevConfig) {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                NbGradleConfiguration newConfig = activeConfig.get();
                changeSupport.firePropertyChange(ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE, prevConfig, newConfig);
                activeConfigChangeListeners.fireEventually();
            }
        });
    }

    public void fireConfigurationListChange() {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                changeSupport.firePropertyChange(ProjectConfigurationProvider.PROP_CONFIGURATIONS, null, null);
            }
        });
    }

    public Collection<NbGradleConfiguration> getConfigurations() {
        ensureLoadedAsynchronously();
        return configs.get();
    }

    public NbGradleConfiguration getActiveConfiguration() {
        ensureLoadedAsynchronously();
        return activeConfig.get();
    }

    private static boolean loadAll(List<ProjectProfileSettings> profiles) {
        for (ProjectProfileSettings profile: profiles) {
            profile.ensureLoadedAndWait();
        }
        return true;
    }

    private void updateByKeyNow(ProfileKey profileKey) {
        ProfileSettingsKey key = new ProfileSettingsKey(rootDirectory, profileKey);
        List<ProjectProfileSettings> profileSettings
                = settingsContainer.getAllProfileSettings(key.getWithFallbacks());
        loadAll(profileSettings);
        multiProfileProperties.setProfileSettings(profileSettings);
    }

    private void updateByKey(final ProfileKey profileKey) {
        // Warning: This method gets called under CONFIG_PROVIDERS_LOCK.
        //          Avoid calling alien code.

        profileApplierExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateByKeyNow(profileKey);
            }
        });
    }

    public void setActiveConfiguration(final NbGradleConfiguration configuration) {
        if (configuration == null) {
            LOGGER.warning("Attempting to set null configuration.");
            return;
        }

        hasActiveBeenSet = true;

        final NbGradleConfiguration prevConfig = activeConfig.getAndSet(configuration);
        if (!prevConfig.equals(configuration)) {
            updateByKey(configuration.getProfileKey());

            fireActiveConfigurationListChange(prevConfig);
        }
        saveActiveProfile();
    }

    public boolean configurationsAffectAction(String command) {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener lst) {
        changeSupport.addPropertyChangeListener(lst);
    }

    public void removePropertyChangeListener(PropertyChangeListener lst) {
        changeSupport.removePropertyChangeListener(lst);
    }

    public ListenerRef addActiveConfigChangeListener(Runnable listener) {
        return activeConfigChangeListeners.registerListener(listener);
    }
}
