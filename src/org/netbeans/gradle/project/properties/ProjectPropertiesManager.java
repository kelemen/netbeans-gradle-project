package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.netbeans.gradle.project.persistent.XmlPropertiesPersister;

public final class ProjectPropertiesManager {
    private static final Lock MAIN_LOCK = new ReentrantLock();
    private static final Map<File, ProjectProperties> PROPERTIES
            = new WeakValueHashMap<File, ProjectProperties>();

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
            File propertiesFile,
            final WaitableSignal loadedSignal) {

        if (propertiesFile == null) throw new NullPointerException("propertiesFile");

        ProjectProperties result;

        MAIN_LOCK.lock();
        try {
            result = PROPERTIES.get(propertiesFile);
        } finally {
            MAIN_LOCK.unlock();
        }

        if (result == null) {
            final ProjectProperties newProperties = new MemProjectProperties();
            final PropertiesPersister persister = new XmlPropertiesPersister(propertiesFile);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    persister.load(newProperties, new Runnable() {
                        @Override
                        public void run() {
                            if (loadedSignal != null) {
                                loadedSignal.signal();
                            }
                            setSaveOnChange(newProperties, persister);
                        }
                    });
                }
            });

            MAIN_LOCK.lock();
            try {
                result = PROPERTIES.get(propertiesFile);
                if (result == null) {
                    PROPERTIES.put(propertiesFile, newProperties);
                    result = newProperties;
                }
            } finally {
                MAIN_LOCK.unlock();
            }
        }
        else {
            if (loadedSignal != null) {
                loadedSignal.signal();
            }
        }

        return result;
    }

    private ProjectPropertiesManager() {
        throw new AssertionError();
    }
}
