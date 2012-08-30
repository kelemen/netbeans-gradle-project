package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.netbeans.gradle.project.persistent.XmlPropertiesPersister;

public final class ProjectPropertiesManager {
    private static final Lock MAIN_LOCK = new ReentrantLock();
    private static final Map<File, ProjectProperties> PROPERTIES
            = new WeakValueHashMap<File, ProjectProperties>();

    public static ProjectProperties getProperties(File propertiesFile) {
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
                    persister.load(newProperties);
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

        return result;
    }

    private ProjectPropertiesManager() {
        throw new AssertionError();
    }
}
