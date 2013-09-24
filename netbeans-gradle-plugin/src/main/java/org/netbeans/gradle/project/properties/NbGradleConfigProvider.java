package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.util.ChangeSupport;

public final class NbGradleConfigProvider implements ProjectConfigurationProvider<NbGradleConfiguration> {
    private static final Logger LOGGER = Logger.getLogger(NbGradleConfigProvider.class.getName());

    private static final String LAST_PROFILE_FILE = "last-profile";

    private static final Lock CONFIG_PROVIDERS_LOCK = new ReentrantLock();
    private static final Map<File, NbGradleConfigProvider> CONFIG_PROVIDERS
            = new WeakValueHashMap<File, NbGradleConfigProvider>();

    private final File rootDirectory;
    private final PropertyChangeSupport changeSupport;
    private final ChangeSupport activeConfigChanges;
    private final AtomicReference<List<NbGradleConfiguration>> configs;
    private final AtomicReference<NbGradleConfiguration> activeConfig;
    private final AtomicBoolean hasBeenUsed;
    private volatile boolean hasActiveBeenSet;

    private NbGradleConfigProvider(File rootDirectory) {
        if (rootDirectory == null) throw new NullPointerException("rootDirectory");

        this.rootDirectory = rootDirectory;
        this.hasBeenUsed = new AtomicBoolean(false);
        this.changeSupport = new PropertyChangeSupport(this);
        this.activeConfigChanges = new ChangeSupport(this);
        this.activeConfig = new AtomicReference<NbGradleConfiguration>(NbGradleConfiguration.DEFAULT_CONFIG);
        this.configs = new AtomicReference<List<NbGradleConfiguration>>(
                Collections.singletonList(NbGradleConfiguration.DEFAULT_CONFIG));
        this.hasActiveBeenSet = false;
    }

    public static NbGradleConfigProvider getConfigProvider(NbGradleProject project) {
        File rootDir = SettingsFiles.getRootDirectory(project);

        // TODO: Add configurations from enabled extensions.
        //   NbGradleConfigProvider is needed to be wrapped for this.

        CONFIG_PROVIDERS_LOCK.lock();
        try {
            NbGradleConfigProvider result = CONFIG_PROVIDERS.get(rootDir);
            if (result == null) {
                result = new NbGradleConfigProvider(rootDir);
                CONFIG_PROVIDERS.put(rootDir, result);
            }
            return result;
        } finally {
            CONFIG_PROVIDERS_LOCK.unlock();
        }
    }

    private void removeFromConfig(NbGradleConfiguration config) {
        List<NbGradleConfiguration> currentList;
        List<NbGradleConfiguration> newList;

        do {
            currentList = configs.get();
            newList = new ArrayList<NbGradleConfiguration>(currentList);
            newList.remove(config);
            newList = Collections.unmodifiableList(newList);
        } while (!configs.compareAndSet(currentList, newList));
    }

    private void addToConfig(Collection<NbGradleConfiguration> toAdd) {
        List<NbGradleConfiguration> currentList;
        List<NbGradleConfiguration> newList;

        do {
            currentList = configs.get();
            Set<NbGradleConfiguration> configSet = new HashSet<NbGradleConfiguration>(currentList);
            configSet.addAll(toAdd);

            newList = new ArrayList<NbGradleConfiguration>(configSet);
            NbGradleConfiguration.sortProfiles(newList);

            newList = Collections.unmodifiableList(newList);
        } while (!configs.compareAndSet(currentList, newList));
    }

    public void removeConfiguration(final NbGradleConfiguration config) {
        if (NbGradleConfiguration.DEFAULT_CONFIG.equals(config)) {
            LOGGER.warning("Cannot remove the default configuration");
            return;
        }

        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                removeFromConfig(config);
                File profileFile = SettingsFiles.getFilesForProfile(rootDirectory, config.getProfileDef())[0];
                if (profileFile.isFile()) {
                    if (!profileFile.delete()) {
                        LOGGER.log(Level.INFO, "Profile was deleted but no profile file was found: {0}", profileFile);
                    }
                }

                executeOnEdt(new Runnable() {
                    @Override
                    public void run() {
                        changeSupport.firePropertyChange(PROP_CONFIGURATIONS, null, null);
                    }
                });
            }
        });
    }

    public void addConfiguration(final NbGradleConfiguration config) {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                addToConfig(Collections.singleton(config));
                changeSupport.firePropertyChange(PROP_CONFIGURATIONS, null, null);
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
                = new ArrayList<NbGradleConfiguration>(profileDefs.size() + 1);

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

        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                changeSupport.firePropertyChange(PROP_CONFIGURATIONS, null, null);
            }
        });
        return configs.get();
    }

    private File getLastProfileFile() {
        return new File(SettingsFiles.getPrivateSettingsDir(rootDirectory), LAST_PROFILE_FILE);
    }

    private void readAndUpdateDefaultProfile() {
        File lastProfileFile = getLastProfileFile();
        if (!lastProfileFile.isFile()) {
            return;
        }

        SavedProfileDef savedDef;
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(lastProfileFile));
            try {
                savedDef = (SavedProfileDef)input.readObject();
            } finally {
                input.close();
            }
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

    private void saveActiveProfileNow() {
        assert NbGradleProject.PROJECT_PROCESSOR.isRequestProcessorThread();

        File lastProfileFile = getLastProfileFile();

        NbGradleConfiguration config = activeConfig.get();
        if (config != null) {
            ProfileDef profileDef = config.getProfileDef();
            if (profileDef == null) {
                if (!lastProfileFile.delete()) {
                    LOGGER.log(Level.FINE, "Last profile file could not be deleted: {0}", lastProfileFile);
                }
                return;
            }

            SavedProfileDef savedDef = new SavedProfileDef(profileDef);

            try {
                File parentFile = lastProfileFile.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }

                ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(lastProfileFile));
                try {
                    output.writeObject(savedDef);
                } finally {
                    output.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Failed to read last profile.", ex);
            }
        }
    }

    private void saveActiveProfile() {
        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                saveActiveProfileNow();
            }
        });
    }

    private void ensureLoadedAsynchronously() {
        if (hasBeenUsed.compareAndSet(false, true)) {
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    findAndUpdateConfigurations(false);
                    readAndUpdateDefaultProfile();
                }
            });
        }
    }

    public void fireConfigurationListChange() {
        executeOnEdt(new Runnable() {
            @Override
            public void run() {
                changeSupport.firePropertyChange(PROP_CONFIGURATIONS, null, null);
            }
        });
    }

    @Override
    public Collection<NbGradleConfiguration> getConfigurations() {
        ensureLoadedAsynchronously();
        return configs.get();
    }

    @Override
    public NbGradleConfiguration getActiveConfiguration() {
        ensureLoadedAsynchronously();
        return activeConfig.get();
    }

    @Override
    public void setActiveConfiguration(final NbGradleConfiguration configuration) {
        if (configuration == null) {
            LOGGER.warning("Attempting to set null configuration.");
            return;
        }

        hasActiveBeenSet = true;

        final NbGradleConfiguration prevConfig = activeConfig.getAndSet(configuration);
        if (!prevConfig.equals(configuration)) {
            executeOnEdt(new Runnable() {
                @Override
                public void run() {
                    changeSupport.firePropertyChange(PROP_CONFIGURATION_ACTIVE, prevConfig, configuration);
                    activeConfigChanges.fireChange();
                }
            });
        }
        saveActiveProfile();
    }

    @Override
    public boolean hasCustomizer() {
        return false;
    }

    @Override
    public void customize() {
    }

    @Override
    public boolean configurationsAffectAction(String command) {
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener lst) {
        changeSupport.addPropertyChangeListener(lst);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener lst) {
        changeSupport.removePropertyChangeListener(lst);
    }

    public void addActiveConfigChangeListener(ChangeListener listener) {
        activeConfigChanges.addChangeListener(listener);
    }

    public void removeActiveConfigChangeListener(ChangeListener listener) {
        activeConfigChanges.removeChangeListener(listener);
    }
}
