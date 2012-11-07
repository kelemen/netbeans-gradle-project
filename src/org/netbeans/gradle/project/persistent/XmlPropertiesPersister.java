package org.netbeans.gradle.project.persistent;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.PropertiesSnapshot;

public final class XmlPropertiesPersister implements PropertiesPersister {
    private final File propertiesFile;

    public XmlPropertiesPersister(File propertiesFile) {
        if (propertiesFile == null) throw new NullPointerException("propertiesFile");

        this.propertiesFile = propertiesFile;
    }

    private void checkEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method may only be called from the EDT.");
        }
    }

    @Override
    public void save(ProjectProperties properties, final Runnable onDone) {
        checkEDT();

        final PropertiesSnapshot snapshot = new PropertiesSnapshot(properties);
        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    XmlPropertyFormat.saveToXml(propertiesFile, snapshot);
                } finally {
                    if (onDone != null) {
                        onDone.run();
                    }
                }
            }
        });
    }

    @Override
    public void load(final ProjectProperties properties, final Runnable onDone) {
        checkEDT();

        // We must listen for changes, so that we do not overwrite properties
        // modified later.
        final ChangeDetector platformChanged = new ChangeDetector();
        final ChangeDetector sourceEncodingChanged = new ChangeDetector();
        final ChangeDetector sourceLevelChanged = new ChangeDetector();
        final ChangeDetector commonTasksChanged = new ChangeDetector();

        properties.getPlatform().addChangeListener(platformChanged);
        properties.getSourceEncoding().addChangeListener(sourceEncodingChanged);
        properties.getSourceLevel().addChangeListener(sourceLevelChanged);
        properties.getCommonTasks().addChangeListener(commonTasksChanged);

        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final PropertiesSnapshot snapshot = XmlPropertyFormat.readFromXml(propertiesFile);

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!sourceLevelChanged.hasChanged()) {
                                properties.getSourceLevel().setValueFromSource(snapshot.getSourceLevel());
                            }
                            if (!platformChanged.hasChanged()) {
                                properties.getPlatform().setValueFromSource(snapshot.getPlatform());
                            }
                            if (!sourceEncodingChanged.hasChanged()) {
                                properties.getSourceEncoding().setValueFromSource(snapshot.getSourceEncoding());
                            }
                            if (!commonTasksChanged.hasChanged()) {
                                properties.getCommonTasks().setValueFromSource(snapshot.getCommonTasks());
                            }

                            if (onDone != null) {
                                onDone.run();
                            }
                        }
                    });

                } finally {
                    // invokeLater is required, so that the listeners will not
                    // be removed before setting the properties.
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            properties.getPlatform().removeChangeListener(platformChanged);
                            properties.getSourceEncoding().removeChangeListener(sourceEncodingChanged);
                            properties.getSourceLevel().removeChangeListener(sourceLevelChanged);
                            properties.getCommonTasks().removeChangeListener(commonTasksChanged);
                        }
                    });
                }

            }
        });
    }

    private static class ChangeDetector implements ChangeListener {
        private volatile boolean changed;

        public boolean hasChanged() {
            return changed;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            changed = true;
        }
    }
}
