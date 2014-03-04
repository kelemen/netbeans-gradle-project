package org.netbeans.gradle.project.properties;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.netbeans.gradle.project.persistent.XmlPropertiesPersister;

public final class ProjectPropertiesManager {
    private static final Logger LOGGER = Logger.getLogger(ProjectPropertiesManager.class.getName());

    private static final Lock MAIN_LOCK = new ReentrantLock();
    // Don't forget that the value can't be wrapped, it must be the one to be
    // returned, otherwise it might get garbage collected.
    private static final Map<ProjectPropertySource, CachedProperties> PROPERTIES
            = new WeakValueHashMap<>();

    private static void saveIfRequired(
            final NbGradleProject project,
            final AtomicBoolean saveQueued,
            final ProjectProperties properties,
            final PropertiesPersister persister) {

        if (saveQueued.compareAndSet(false, true)) {
            PropertiesPersister.PERSISTER_PROCESSOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    saveQueued.set(false);
                    persister.save(project, properties, null);
                }
            }, null);
        }
    }

    private static void setSaveOnChange(
            final NbGradleProject project,
            final ProjectProperties properties,
            final PropertiesPersister persister) {

        final AtomicBoolean saveQueued = new AtomicBoolean(false);

        Runnable saveIfRequiredTask = new Runnable() {
            @Override
            public void run() {
                saveIfRequired(project, saveQueued, properties, persister);
            }
        };

        for (OldMutableProperty<?> property: properties.getAllProperties()) {
            property.addChangeListener(saveIfRequiredTask);
        }
    }

    public static ProjectProperties getProperties(
            NbGradleProject project,
            final WaitableSignal loadedSignal) {
        ExceptionHelper.checkNotNullArgument(loadedSignal, "loadedSignal");
        return getPropertySourceForProject(project).load(new PropertiesLoadListener() {
            @Override
            public void loadedProperties(ProjectProperties properties) {
                loadedSignal.signal();
            }
        });
    }

    public static ProjectPropertySource getPropertySourceForProject(
            NbGradleProject project, ProfileDef profileDef) {
        return new NbProfileProjectPropertySource(project, profileDef);
    }

    public static ProjectPropertySource getPropertySourceForProject(NbGradleProject project) {
        return new NbCurrentProfileProjectPropertySource(project);
    }

    public static ProjectPropertySource getFilesPropertySource(NbGradleProject project, File... propertiesFiles) {
        return getFilesPropertySource(project, Arrays.asList(propertiesFiles));
    }

    public static ProjectPropertySource getFilesPropertySource(NbGradleProject project, List<File> propertiesFiles) {
        List<ProjectPropertySource> sources = new ArrayList<>();
        for (File propertyFile: propertiesFiles) {
            sources.add(getFilePropertySource(project, propertyFile));
        }
        return combineSources(sources);
    }

    public static ProjectPropertySource getFilePropertySource(NbGradleProject project, final File propertiesFile) {
        return new FileProjectPropertySource(project, propertiesFile);
    }

    public static ProjectPropertySource combineSources(List<? extends ProjectPropertySource> propertySources) {
        return combineSources(propertySources.toArray(new ProjectPropertySource[propertySources.size()]), 0);
    }

    public static ProjectPropertySource combineSources(ProjectPropertySource... propertySources) {
        return combineSources(propertySources, 0);
    }

    private static ProjectPropertySource combineSources(
            final ProjectPropertySource[] propertySources,
            final int offset) {

        return new CombinedProjectPropertySource(propertySources, offset);
    }

    private static CachedProperties loadPropertiesAlways(
            final NbGradleProject project,
            File propertiesFile) {

        final CachedProperties result = new CachedProperties(new MemProjectProperties());
        final PropertiesPersister persister = new XmlPropertiesPersister(propertiesFile);
        PropertiesPersister.PERSISTER_PROCESSOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                persister.load(result, false, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            result.signalPropertiesLoaded();
                        } finally {
                            setSaveOnChange(project, result, persister);
                        }
                    }
                });
            }
        }, null);
        return result;
    }

    private static class FileProjectPropertySource implements ProjectPropertySource {
        private final NbGradleProject project;
        private final File propertiesFile;

        public FileProjectPropertySource(NbGradleProject project, File propertiesFile) {
            ExceptionHelper.checkNotNullArgument(propertiesFile, "propertiesFile");
            this.project = project;
            this.propertiesFile = propertiesFile;
        }

        @Override
        public ProjectProperties load(PropertiesLoadListener onLoadTask) {
            CachedProperties result;
            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(this);
            } finally {
                MAIN_LOCK.unlock();
            }

            if (result == null) {
                final CachedProperties newProperties = loadPropertiesAlways(project, propertiesFile);

                MAIN_LOCK.lock();
                try {
                    result = PROPERTIES.get(this);
                    if (result == null) {
                        PROPERTIES.put(this, newProperties);
                        result = newProperties;
                    }
                } finally {
                    MAIN_LOCK.unlock();
                }
            }

            assert result != null;

            if (onLoadTask != null) {
                result.notifyOnLoad(onLoadTask);
            }

            return result;
        }

        @Override
        public int hashCode() {
            return 371 + propertiesFile.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final FileProjectPropertySource other = (FileProjectPropertySource)obj;
            return this.propertiesFile == other.propertiesFile
                    || this.propertiesFile.equals(other.propertiesFile);
        }
    }

    private static class CombinedProjectPropertySource implements ProjectPropertySource {
        private final ProjectPropertySource[] propertySources;

        public CombinedProjectPropertySource(ProjectPropertySource[] propertySources, int offset) {
            this.propertySources = Arrays.copyOfRange(propertySources, offset, propertySources.length);

            ExceptionHelper.checkNotNullElements(this.propertySources, "propertySources");
        }

        @Override
        public ProjectProperties load(final PropertiesLoadListener onLoadTask) {
            CachedProperties result;
            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(this);
            } finally {
                MAIN_LOCK.unlock();
            }

            if (result == null) {
                final AtomicReference<CachedProperties> resultRef
                        = new AtomicReference<>(null);

                final AtomicInteger subTaskCount = new AtomicInteger();
                // Setting the value of resultRef is counted as a subTask as well.
                // This resultForwarder will call onLoadTask only after all
                // properties have been loaded.
                final PropertiesLoadListener resultForwarder = new PropertiesLoadListener() {
                    @Override
                    public void loadedProperties(ProjectProperties properties) {
                        if (subTaskCount.decrementAndGet() == 0 && onLoadTask != null) {
                            CachedProperties loadedProperties = resultRef.get();
                            if (loadedProperties == null) {
                                String message = "Internal error while loading properties.";
                                LOGGER.log(Level.SEVERE, message, new IllegalStateException(message));
                                return;
                            }
                            loadedProperties.signalPropertiesLoaded();
                        }
                    }
                };

                ProjectProperties newProperties;
                if (propertySources.length == 1) {
                    subTaskCount.set(2);
                    newProperties = propertySources[0].load(onLoadTask);
                }
                else {
                    subTaskCount.set(3);

                    ProjectProperties mainProperties = propertySources[0].load(resultForwarder);
                    ProjectProperties fallbackProperties
                            = combineSources(propertySources, 1).load(resultForwarder);

                    newProperties = new FallbackProjectProperties(mainProperties, fallbackProperties);
                }

                CachedProperties cachedNewProperties = new CachedProperties(newProperties);
                MAIN_LOCK.lock();
                try {
                    result = PROPERTIES.get(this);
                    if (result == null) {
                        PROPERTIES.put(this, cachedNewProperties);
                        result = cachedNewProperties;
                    }
                } finally {
                    MAIN_LOCK.unlock();
                }

                resultRef.set(result);
                resultForwarder.loadedProperties(result);
            }

            if (onLoadTask != null) {
                result.notifyOnLoad(onLoadTask);
            }

            return result;
        }

        @Override
        public int hashCode() {
            return 581 + Arrays.hashCode(this.propertySources);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final CombinedProjectPropertySource other = (CombinedProjectPropertySource)obj;
            return Arrays.equals(this.propertySources, other.propertySources);
        }
    }

    private static ProjectPropertySource filesWithDefault(
            NbGradleProject project, File[] files, ProjectPropertySource defaultSource) {

        ProjectPropertySource[] sources = new ProjectPropertySource[files.length + 1];
        for (int i = 0; i < files.length; i++) {
            sources[i] = getFilePropertySource(project, files[i]);
        }
        sources[files.length] = defaultSource;

        return combineSources(sources);
    }

    private static final class NbProfileProjectPropertySource implements ProjectPropertySource {
        private final NbGradleProject project;
        private final ProfileDef profileDef;
        private final ProjectPropertySource defaultSource;

        public NbProfileProjectPropertySource(NbGradleProject project, ProfileDef profileDef) {
            ExceptionHelper.checkNotNullArgument(project, "project");

            this.project = project;
            this.profileDef = profileDef;
            this.defaultSource = new DefaultProjectPropertySource(project);
        }

        @Override
        public ProjectProperties load(final PropertiesLoadListener onLoadTask) {
            File[] files = SettingsFiles.getFilesForProfile(project, profileDef);
            return filesWithDefault(project, files, defaultSource).load(onLoadTask);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 73 * hash + project.hashCode();
            hash = 73 * hash + Objects.hashCode(profileDef);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final NbProfileProjectPropertySource other = (NbProfileProjectPropertySource)obj;

            return Objects.equals(this.project, other.project)
                    && Objects.equals(this.profileDef, other.profileDef);
        }
    }

    private static final class NbCurrentProfileProjectPropertySource implements ProjectPropertySource {
        private final NbGradleProject project;
        private final ProjectPropertySource defaultSource;

        public NbCurrentProfileProjectPropertySource(NbGradleProject project) {
            ExceptionHelper.checkNotNullArgument(project, "project");

            this.project = project;
            this.defaultSource = new DefaultProjectPropertySource(project);
        }

        @Override
        public ProjectProperties load(final PropertiesLoadListener onLoadTask) {
            File[] files = SettingsFiles.getFilesForProject(project);
            return filesWithDefault(project, files, defaultSource).load(onLoadTask);
        }

        @Override
        public int hashCode() {
            return 335 + this.project.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final NbCurrentProfileProjectPropertySource other = (NbCurrentProfileProjectPropertySource)obj;
            return this.project.equals(other.project);
        }
    }

    private static final class CachedProperties implements ProjectProperties {
        private final ProjectProperties properties;
        private final Lock loadLock;
        private final List<PropertiesLoadListener> onLoadedTask;
        private volatile boolean loaded;

        public CachedProperties(ProjectProperties properties) {
            assert properties != null;
            this.properties = properties;
            this.loadLock = new ReentrantLock();
            this.onLoadedTask = new LinkedList<>();
        }

        public void signalPropertiesLoaded() {
            PropertiesLoadListener[] toNotify;
            loadLock.lock();
            try {
                loaded = true;
                if (onLoadedTask.isEmpty()) {
                    return;
                }

                toNotify = onLoadedTask.toArray(new PropertiesLoadListener[onLoadedTask.size()]);
                onLoadedTask.clear();
            } finally {
                loadLock.unlock();
            }

            for (PropertiesLoadListener listener: toNotify) {
                listener.loadedProperties(properties);
            }
        }

        public void notifyOnLoad(PropertiesLoadListener listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            if (loaded) {
                listener.loadedProperties(properties);
                return;
            }

            loadLock.lock();
            try {
                if (!loaded) {
                    onLoadedTask.add(listener);
                    return;
                }
            } finally {
                loadLock.unlock();
            }

            listener.loadedProperties(properties);
        }

        @Override
        public OldMutableProperty<String> getSourceLevel() {
            return properties.getSourceLevel();
        }

        @Override
        public OldMutableProperty<ProjectPlatform> getPlatform() {
            return properties.getPlatform();
        }

        @Override
        public OldMutableProperty<JavaPlatform> getScriptPlatform() {
            return properties.getScriptPlatform();
        }

        @Override
        public OldMutableProperty<GradleLocation> getGradleLocation() {
            return properties.getGradleLocation();
        }

        @Override
        public OldMutableProperty<Charset> getSourceEncoding() {
            return properties.getSourceEncoding();
        }

        @Override
        public OldMutableProperty<List<PredefinedTask>> getCommonTasks() {
            return properties.getCommonTasks();
        }

        @Override
        public OldMutableProperty<LicenseHeaderInfo> getLicenseHeader() {
            return properties.getLicenseHeader();
        }

        @Override
        public OldMutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
            return properties.tryGetBuiltInTask(command);
        }

        @Override
        public Set<String> getKnownBuiltInCommands() {
            return properties.getKnownBuiltInCommands();
        }

        @Override
        public OldMutableProperty<Void> getAuxConfigListener() {
            return properties.getAuxConfigListener();
        }

        @Override
        public AuxConfigProperty getAuxConfig(String elementName, String namespace) {
            return properties.getAuxConfig(elementName, namespace);
        }

        @Override
        public void setAllAuxConfigs(Collection<AuxConfig> configs) {
            properties.setAllAuxConfigs(configs);
        }

        @Override
        public Collection<AuxConfigProperty> getAllAuxConfigs() {
            return properties.getAllAuxConfigs();
        }

        @Override
        public Collection<OldMutableProperty<?>> getAllProperties() {
            return properties.getAllProperties();
        }
    }

    private ProjectPropertiesManager() {
        throw new AssertionError();
    }
}
