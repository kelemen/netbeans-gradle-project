package org.netbeans.gradle.project.properties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.MonitorableTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.SerializationUtils2;

public final class NbGradleConfigProvider {
    private static final Logger LOGGER = Logger.getLogger(NbGradleConfigProvider.class.getName());

    // Must be FIFO executor
    private static final TaskExecutor PROFILE_APPLIER_EXECUTOR
            = NbTaskExecutors.newExecutor("Profile-applier", 1);

    private static final String LAST_PROFILE_FILE = "last-profile";

    private static final Lock CONFIG_PROVIDERS_LOCK = new ReentrantLock();
    private static final Map<Path, NbGradleConfigProvider> CONFIG_PROVIDERS
            = new WeakValueHashMap<>();

    private final Path rootDirectory;
    private final ChangeListenerManager activeConfigChangeListeners;
    private final ChangeListenerManager configsChangeListeners;
    private final AtomicReference<List<NbGradleConfiguration>> configs;
    private final AtomicReference<NbGradleConfiguration> activeConfigRef;

    private final MultiProfileProperties multiProfileProperties;
    private final ProfileSettingsContainer settingsContainer;

    private final UpdateTaskExecutor profileApplierExecutor;

    private final MonitorableTaskExecutor profileIOExecutor;

    private final MutableProperty<NbGradleConfiguration> activeConfiguration;
    private final PropertySource<Collection<NbGradleConfiguration>> configurations;

    private NbGradleConfigProvider(
            Path rootDirectory,
            NbGradleConfiguration selectedConfig,
            List<NbGradleConfiguration> initialConfigs,
            MultiProfileProperties multiProfileProperties,
            ProfileSettingsContainer settingsContainer) {

        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        this.activeConfigChangeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.configsChangeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.activeConfigRef = new AtomicReference<>(selectedConfig);
        this.configs = new AtomicReference<>(CollectionsEx.readOnlyCopy(initialConfigs));
        this.multiProfileProperties = Objects.requireNonNull(multiProfileProperties, "multiProfileProperties");
        this.settingsContainer = Objects.requireNonNull(settingsContainer, "settingsContainer");
        this.profileApplierExecutor = new GenericUpdateTaskExecutor(PROFILE_APPLIER_EXECUTOR);
        this.profileIOExecutor = NbTaskExecutors.newDefaultFifoExecutor();

        this.configurations = NbProperties.<Collection<NbGradleConfiguration>>atomicValueView(configs, configsChangeListeners);

        this.activeConfiguration = new MutableProperty<NbGradleConfiguration>() {
            @Override
            public void setValue(NbGradleConfiguration config) {
                setActiveConfiguration(config);
            }

            @Override
            public NbGradleConfiguration getValue() {
                return getActiveConfiguration();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return activeConfigChangeListeners.registerListener(listener);
            }
        };
    }

    private static NbGradleConfigProvider tryGetConfigProvider(Path rootDir) {
        CONFIG_PROVIDERS_LOCK.lock();
        try {
            return CONFIG_PROVIDERS.get(rootDir);
        } finally {
            CONFIG_PROVIDERS_LOCK.unlock();
        }
    }

    public static NbGradleConfigProvider getConfigProvider(Path rootDir) {
        NbGradleConfigProvider result = tryGetConfigProvider(rootDir);
        if (result != null) {
            return result;
        }

        // This path is usually only taken on the first load of the config.
        // There is a chance that it might get loaded again in some rare
        // cases but then we will detect it later and discard the config
        // reading done the second time.

        List<NbGradleConfiguration> availableConfigs = readAvailableConfigs(rootDir);
        NbGradleConfiguration initialConfig = readLastSelectedProfile(rootDir, availableConfigs);

        ProfileSettingsContainer settingsContainer = ProfileSettingsContainer.getDefault();
        List<SingleProfileSettingsEx> initialProfiles = getLoadedProfileSettings(rootDir,
                settingsContainer,
                initialConfig.getProfileKey());

        MultiProfileProperties profileProperties = new MultiProfileProperties(initialProfiles);

        result = new NbGradleConfigProvider(rootDir,
                initialConfig,
                availableConfigs,
                profileProperties,
                settingsContainer);

        CONFIG_PROVIDERS_LOCK.lock();
        try {
            NbGradleConfigProvider currentProvider = CONFIG_PROVIDERS.get(rootDir);
            if (currentProvider == null) {
                CONFIG_PROVIDERS.put(rootDir, result);
            }
            else {
                result = currentProvider;
            }
        } finally {
            CONFIG_PROVIDERS_LOCK.unlock();
        }

        return result;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public ActiveSettingsQueryEx getActiveSettingsQuery() {
        return multiProfileProperties;
    }

    public ProfileSettingsContainer getProfileSettingsContainer() {
        return settingsContainer;
    }

    private void removeFromConfig(NbGradleConfiguration config) {
        configs.updateAndGet(currentList -> {
            currentList = configs.get();
            List<NbGradleConfiguration> newList = new ArrayList<>(currentList);
            newList.remove(config);
            return Collections.unmodifiableList(newList);
        });
    }

    private void addToConfig(Collection<NbGradleConfiguration> toAdd) {
        configs.updateAndGet(currentList -> {
            Set<NbGradleConfiguration> configSet = new HashSet<>(currentList);
            configSet.addAll(toAdd);

            List<NbGradleConfiguration> newList = new ArrayList<>(configSet);
            NbGradleConfiguration.sortProfiles(newList);

            return Collections.unmodifiableList(newList);
        });
    }

    public void removeConfiguration(final NbGradleConfiguration config) {
        if (NbGradleConfiguration.DEFAULT_CONFIG.equals(config)) {
            LOGGER.warning("Cannot remove the default configuration");
            return;
        }

        profileIOExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            removeFromConfig(config);
            Path profileFile = SettingsFiles.getFilesForProfile(rootDirectory, config.getProfileDef())[0];
            if (!Files.deleteIfExists(profileFile)) {
                LOGGER.log(Level.INFO, "Profile was deleted but no profile file was found: {0}", profileFile);
            }

            fireConfigurationListChange();
        }).exceptionally(AsyncTasks::expectNoError);
    }

    public void addConfiguration(final NbGradleConfiguration config) {
        executeOnEdt(() -> {
            addToConfig(Collections.singleton(config));
            fireConfigurationListChange();
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

    private static List<NbGradleConfiguration> readAvailableConfigs(Path rootDir) {
        Collection<ProfileDef> profileDefs = SettingsFiles.getAvailableProfiles(rootDir);
        List<NbGradleConfiguration> result = new ArrayList<>(profileDefs.size() + 1);

        result.add(NbGradleConfiguration.DEFAULT_CONFIG);
        for (ProfileDef profileDef: profileDefs) {
            result.add(new NbGradleConfiguration(profileDef));
        }

        return result;
    }

    private Path getLastProfileFile() {
        return getLastProfileFile(rootDirectory);
    }

    private static Path getLastProfileFile(Path rootDirectory) {
        return SettingsFiles.getPrivateSettingsDir(rootDirectory).resolve(LAST_PROFILE_FILE);
    }

    private static NbGradleConfiguration readLastSelectedProfile(
            Path rootDirectory,
            List<NbGradleConfiguration> availableConfigs) {
        NbGradleConfiguration result = tryReadLastSelectedProfile(rootDirectory, availableConfigs);
        return result != null ? result : NbGradleConfiguration.DEFAULT_CONFIG;
    }

    private static NbGradleConfiguration tryReadLastSelectedProfile(
            Path rootDirectory,
            List<NbGradleConfiguration> availableConfigs) {

        Path lastProfileFile = getLastProfileFile(rootDirectory);
        if (!Files.isRegularFile(lastProfileFile)) {
            return null;
        }

        SavedProfileDef savedDef;
        try {
            savedDef = (SavedProfileDef)SerializationUtils2.deserializeFile(lastProfileFile);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to read last profile.", ex);
            return null;
        }

        return savedDef.findSameConfig(availableConfigs);
    }

    private void saveActiveProfileNow() throws IOException {
        assert profileIOExecutor.isExecutingInThis();

        Path lastProfileFile = getLastProfileFile();

        NbGradleConfiguration config = activeConfigRef.get();
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
        profileIOExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            saveActiveProfileNow();
        }).exceptionally(AsyncTasks::expectNoError);
    }

    private void fireActiveConfigurationListChange() {
        activeConfigChangeListeners.fireEventually();
    }

    private void fireConfigurationListChange() {
        configsChangeListeners.fireEventually();
    }

    public Collection<NbGradleConfiguration> getConfigurations() {
        return configs.get();
    }

    public NbGradleConfiguration getActiveConfiguration() {
        return activeConfigRef.get();
    }

    private static List<SingleProfileSettingsEx> getLoadedProfileSettings(
            Path rootDirectory,
            ProfileSettingsContainer settingsContainer,
            ProfileKey profileKey) {

        ProfileSettingsKey key = ProjectProfileSettingsKey.getForProject(rootDirectory, profileKey);
        List<SingleProfileSettingsEx> profileSettings
                = settingsContainer.loadAllProfileSettings(key.getWithFallbacks());
        return profileSettings;
    }

    private List<SingleProfileSettingsEx> getLoadedProfileSettings(ProfileKey profileKey) {
        return getLoadedProfileSettings(rootDirectory, settingsContainer, profileKey);
    }

    private void updateByKeyNow(ProfileKey profileKey) {
        multiProfileProperties.setProfileSettings(getLoadedProfileSettings(profileKey));
    }

    private void updateByKey(final ProfileKey profileKey) {
        profileApplierExecutor.execute(() -> updateByKeyNow(profileKey));
    }

    public void setActiveConfiguration(final NbGradleConfiguration configuration) {
        if (configuration == null) {
            LOGGER.warning("Attempting to set null configuration.");
            return;
        }

        final NbGradleConfiguration prevConfig = activeConfigRef.getAndSet(configuration);
        if (!prevConfig.equals(configuration)) {
            updateByKey(configuration.getProfileKey());

            fireActiveConfigurationListChange();
        }
        saveActiveProfile();
    }

    public boolean configurationsAffectAction(String command) {
        return true;
    }

    public PropertySource<Collection<NbGradleConfiguration>> configurations() {
        return configurations;
    }

    public MutableProperty<NbGradleConfiguration> activeConfiguration() {
        return activeConfiguration;
    }

    public ListenerRef addActiveConfigChangeListener(Runnable listener) {
        return activeConfigChangeListeners.registerListener(listener);
    }
}
