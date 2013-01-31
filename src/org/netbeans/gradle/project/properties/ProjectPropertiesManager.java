package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.netbeans.gradle.project.persistent.XmlPropertiesPersister;

public final class ProjectPropertiesManager {
    private static final Logger LOGGER = Logger.getLogger(ProjectPropertiesManager.class.getName());

    private static final Lock MAIN_LOCK = new ReentrantLock();
    private static final Map<ProjectPropertySource, ProjectProperties> PROPERTIES
            = new WeakValueHashMap<ProjectPropertySource, ProjectProperties>();

    private static void saveIfRequired(
            final AtomicBoolean saveQueued,
            final ProjectProperties properties,
            final PropertiesPersister persister) {

        if (saveQueued.compareAndSet(false, true)) {
            PropertiesPersister.PERSISTER_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    saveQueued.set(false);
                    persister.save(properties, null);
                }
            });
        }
    }

    private static void setSaveOnChange(
            final ProjectProperties properties,
            final PropertiesPersister persister) {

        final AtomicBoolean saveQueued = new AtomicBoolean(false);

        ChangeListener saveIfRequiredTask = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                saveIfRequired(saveQueued, properties, persister);
            }
        };

        for (MutableProperty<?> property: properties.getAllProperties()) {
            property.addChangeListener(saveIfRequiredTask);
        }
    }

    public static ProjectProperties getProperties(
            NbGradleProject project,
            final WaitableSignal loadedSignal) {
        if (loadedSignal == null) throw new NullPointerException("loadedSignal");
        return getPropertySourceForProject(project).load(new PropertiesLoadListener() {
            @Override
            public void loadedProperties(ProjectProperties properties) {
                loadedSignal.signal();
            }
        });
    }

    public static ProjectPropertySource getPropertySourceForProject(
            NbGradleProject project, String profileName) {
        return new NbProfileProjectPropertySource(project, profileName);
    }

    public static ProjectPropertySource getPropertySourceForProject(NbGradleProject project) {
        return new NbCurrentProfileProjectPropertySource(project);
    }

    public static ProjectPropertySource getFilesPropertySource(File... propertiesFiles) {
        return getFilesPropertySource(Arrays.asList(propertiesFiles));
    }

    public static ProjectPropertySource getFilesPropertySource(List<File> propertiesFiles) {
        List<ProjectPropertySource> sources = new ArrayList<ProjectPropertySource>();
        for (File propertyFile: propertiesFiles) {
            sources.add(getFilePropertySource(propertyFile));
        }
        return combineSources(sources);
    }

    public static ProjectPropertySource getFilePropertySource(final File propertiesFile) {
        return new FileProjectPropertySource(propertiesFile);
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

    private static ProjectProperties loadPropertiesAlways(
            File propertiesFile,
            final PropertiesLoadListener onLoadTask) {

        final ProjectProperties properties = new MemProjectProperties();
        final PropertiesPersister persister = new XmlPropertiesPersister(propertiesFile);
        PropertiesPersister.PERSISTER_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                persister.load(properties, false, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (onLoadTask != null) {
                                onLoadTask.loadedProperties(properties);
                            }
                        } finally {
                            setSaveOnChange(properties, persister);
                        }
                    }
                });
            }
        });
        return properties;
    }

    private static class FileProjectPropertySource implements ProjectPropertySource {
        private final File propertiesFile;

        public FileProjectPropertySource(File propertiesFile) {
            if (propertiesFile == null) throw new NullPointerException("propertiesFile");
            this.propertiesFile = propertiesFile;
        }

        @Override
        public ProjectProperties load(PropertiesLoadListener onLoadTask) {
            ProjectProperties result;
            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(this);
            } finally {
                MAIN_LOCK.unlock();
            }

            if (result == null) {
                final ProjectProperties newProperties = loadPropertiesAlways(propertiesFile, onLoadTask);

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
            else {
                if (onLoadTask != null) {
                    onLoadTask.loadedProperties(result);
                }
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
            for (ProjectPropertySource source: this.propertySources) {
                if (source == null) throw new NullPointerException("propertySource");
            }
        }

        @Override
        public ProjectProperties load(final PropertiesLoadListener onLoadTask) {
            ProjectProperties result;
            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(this);
            } finally {
                MAIN_LOCK.unlock();
            }

            if (result == null) {
                final AtomicReference<ProjectProperties> resultRef
                        = new AtomicReference<ProjectProperties>(null);

                final AtomicInteger subTaskCount = new AtomicInteger();
                // Setting the value of resultRef is counted as a subTask as well.
                // This resultForwarder will call onLoadTask only after all
                // properties have been loaded.
                final PropertiesLoadListener resultForwarder = new PropertiesLoadListener() {
                    @Override
                    public void loadedProperties(ProjectProperties properties) {
                        if (subTaskCount.decrementAndGet() == 0 && onLoadTask != null) {
                            ProjectProperties loadedProperties = resultRef.get();
                            if (loadedProperties == null) {
                                String message = "Internal error while loading properties.";
                                LOGGER.log(Level.SEVERE, message, new IllegalStateException(message));
                                return;
                            }
                            onLoadTask.loadedProperties(loadedProperties);
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

                resultRef.set(result);
                resultForwarder.loadedProperties(result);
            }
            else {
                if (onLoadTask != null) {
                    onLoadTask.loadedProperties(result);
                }
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

    private static ProjectPropertySource filesWithDefault(File[] files, ProjectPropertySource defaultSource) {
        ProjectPropertySource[] sources = new ProjectPropertySource[files.length + 1];
        for (int i = 0; i < files.length; i++) {
            sources[i] = getFilePropertySource(files[i]);
        }
        sources[files.length] = defaultSource;

        return combineSources(sources);
    }

    private static final class NbProfileProjectPropertySource implements ProjectPropertySource {
        private final NbGradleProject project;
        private final String profileName;
        private final ProjectPropertySource defaultSource;

        public NbProfileProjectPropertySource(NbGradleProject project, String profileName) {
            if (project == null) throw new NullPointerException("project");

            this.project = project;
            this.profileName = profileName;
            this.defaultSource = new DefaultProjectPropertySource(project);
        }

        @Override
        public ProjectProperties load(final PropertiesLoadListener onLoadTask) {
            File[] files = SettingsFiles.getFilesForProfile(project, profileName);
            return filesWithDefault(files, defaultSource).load(onLoadTask);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 73 * hash + this.project.hashCode();
            hash = 73 * hash + (this.profileName != null ? this.profileName.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final NbProfileProjectPropertySource other = (NbProfileProjectPropertySource)obj;
            if (!this.project.equals(other.project)) return false;
            if ((this.profileName == null) ? (other.profileName != null) : !this.profileName.equals(other.profileName)) {
                return false;
            }
            return true;
        }
    }

    private static final class NbCurrentProfileProjectPropertySource implements ProjectPropertySource {
        private final NbGradleProject project;
        private final ProjectPropertySource defaultSource;

        public NbCurrentProfileProjectPropertySource(NbGradleProject project) {
            if (project == null) throw new NullPointerException("project");

            this.project = project;
            this.defaultSource = new DefaultProjectPropertySource(project);
        }

        @Override
        public ProjectProperties load(final PropertiesLoadListener onLoadTask) {
            File[] files = SettingsFiles.getFilesForProject(project);
            return filesWithDefault(files, defaultSource).load(onLoadTask);
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

    private ProjectPropertiesManager() {
        throw new AssertionError();
    }
}
