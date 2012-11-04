package org.netbeans.gradle.project.properties;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.netbeans.gradle.project.persistent.XmlPropertiesPersister;

public final class ProjectPropertiesManager {
    private static final Lock MAIN_LOCK = new ReentrantLock();
    private static final Map<List<File>, ProjectProperties> PROPERTIES
            = new WeakValueHashMap<List<File>, ProjectProperties>();

    private static void saveIfRequired(
            final AtomicBoolean saveQueued,
            final ProjectProperties properties,
            final PropertiesPersister persister) {

        if (saveQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(new Runnable() {
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
            File[] propertiesFiles,
            final WaitableSignal loadedSignal) {
        if (loadedSignal != null) {
            return getProperties(propertiesFiles, new Runnable() {
                @Override
                public void run() {
                    loadedSignal.signal();
                }
            });
        }
        else {
            return getProperties(propertiesFiles, (Runnable)null);
        }
    }

    public static ProjectProperties getProperties(
            File[] propertiesFiles,
            Runnable onLoadTask) {
        return getProperties(Arrays.asList(propertiesFiles), onLoadTask);
    }

    public static ProjectProperties getProperties(
            List<File> propertiesFiles,
            final Runnable onLoadTask) {

        List<File> fileList = new ArrayList<File>(propertiesFiles);
        for (File file: fileList) {
            if (file == null) throw new NullPointerException("file");
        }
        if (fileList.isEmpty()) {
            throw new IllegalArgumentException("file list is empty.");
        }

        ProjectProperties result;
        MAIN_LOCK.lock();
        try {
            result = PROPERTIES.get(fileList);
        } finally {
            MAIN_LOCK.unlock();
        }

        if (result == null) {
            ProjectProperties newProperties;
            if (fileList.size() == 1) {
                newProperties = getProperties(fileList.get(0), onLoadTask);
            }
            else {
                Runnable runLastTask;
                if (onLoadTask != null) {
                    // This task runs "onLoadTask", only after the second run.
                    // That is, after both properties have been loaded.
                    runLastTask = new Runnable() {
                        private final AtomicBoolean runnedOnce = new AtomicBoolean(false);

                        @Override
                        public void run() {
                            if (runnedOnce.getAndSet(true)) {
                                onLoadTask.run();
                            }
                        }
                    };
                }
                else {
                    runLastTask = null;
                }

                ProjectProperties mainProperties = getProperties(fileList.get(0), runLastTask);
                ProjectProperties fallbackProperties = getProperties(fileList.subList(1, fileList.size()), runLastTask);

                newProperties = new FallbackProjectProperties(mainProperties, fallbackProperties);

                // Note: We have to keep hard references to the returned
                //   properties to prevent them from being garbage collected and
                //   be removed from "PROPERTIES" while their properties are
                //   still being used (FallbackProjectProperties does not keep
                //   hard references to them).
                newProperties = new ProjectPropertiesWithHardRefs(newProperties, mainProperties, fallbackProperties);
            }


            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(fileList);
                if (result == null) {
                    PROPERTIES.put(fileList, newProperties);
                    result = newProperties;
                }
            } finally {
                MAIN_LOCK.unlock();
            }
        }
        else {
            if (onLoadTask != null) {
                onLoadTask.run();
            }
        }

        return result;
    }

    public static ProjectProperties getProperties(
            File propertiesFile,
            final Runnable onLoadTask) {

        if (propertiesFile == null) throw new NullPointerException("propertiesFile");

        ProjectProperties result;

        List<File> fileList = Collections.singletonList(propertiesFile);

        MAIN_LOCK.lock();
        try {
            result = PROPERTIES.get(fileList);
        } finally {
            MAIN_LOCK.unlock();
        }

        if (result == null) {
            final ProjectProperties newProperties = loadPropertiesAlways(propertiesFile, onLoadTask);

            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(fileList);
                if (result == null) {
                    PROPERTIES.put(fileList, newProperties);
                    result = newProperties;
                }
            } finally {
                MAIN_LOCK.unlock();
            }
        }
        else {
            if (onLoadTask != null) {
                onLoadTask.run();
            }
        }

        return result;
    }

    private static ProjectProperties loadPropertiesAlways(
            File propertiesFile,
            final Runnable onLoadTask) {

        final ProjectProperties properties = new MemProjectProperties();
        final PropertiesPersister persister = new XmlPropertiesPersister(propertiesFile);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                persister.load(properties, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (onLoadTask != null) {
                                onLoadTask.run();
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

    private static class ProjectPropertiesWithHardRefs extends AbstractProjectProperties {
        private final ProjectProperties wrapped;
        private final Object[] hardReferences;

        public ProjectPropertiesWithHardRefs(ProjectProperties wrapped, Object... hardReferences) {
            assert wrapped != null;

            this.wrapped = wrapped;
            this.hardReferences = hardReferences.clone();
        }

        @Override
        public MutableProperty<String> getSourceLevel() {
            return wrapped.getSourceLevel();
        }

        @Override
        public MutableProperty<JavaPlatform> getPlatform() {
            return wrapped.getPlatform();
        }

        @Override
        public MutableProperty<Charset> getSourceEncoding() {
            return wrapped.getSourceEncoding();
        }

        @Override
        public MutableProperty<List<PredefinedTask>> getCommonTasks() {
            return wrapped.getCommonTasks();
        }

        @Override
        public String toString() {
            return "ProjectPropertiesWithHardRefs{"
                    + "wrapped=" + wrapped
                    + ", hardReferences=" + Arrays.toString(hardReferences).hashCode() + '}';
        }
    }

    private ProjectPropertiesManager() {
        throw new AssertionError();
    }
}
