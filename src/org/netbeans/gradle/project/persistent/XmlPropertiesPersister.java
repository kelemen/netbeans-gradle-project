package org.netbeans.gradle.project.persistent;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.PropertiesSnapshot;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class XmlPropertiesPersister implements PropertiesPersister {
    private static final String SETTINGS_FILENAME = ".nb-gradle-properties";
    private static final String PROFILE_FILE_NAME_SUFFIX = ".profile";
    private static final String SETTINGS_DIR_NAME = ".nb-gradle";
    private static final String PROFILE_DIRECTORY = "profiles";

    private final File propertiesFile;

    public XmlPropertiesPersister(File propertiesFile) {
        if (propertiesFile == null) throw new NullPointerException("propertiesFile");

        this.propertiesFile = propertiesFile;
    }

    public static Collection<String> getAvailableProfiles(NbGradleProject project) {
        File profileDir = getProfileDirectory(project);
        if (!profileDir.isDirectory()) {
            return Collections.emptySet();
        }

        File[] profileFiles = profileDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ROOT).endsWith(PROFILE_FILE_NAME_SUFFIX);
            }
        });

        if (profileFiles == null) {
            return Collections.emptySet();
        }

        List<String> result = new ArrayList<String>(profileFiles.length);
        int suffixLength = PROFILE_FILE_NAME_SUFFIX.length();
        for (File profileFile: profileFiles) {
            String fileName = profileFile.getName();
            if (fileName.length() >= suffixLength) {
                result.add(fileName.substring(0, fileName.length() - suffixLength));
            }
        }
        return result;
    }

    public static File getSettingsDir(NbGradleProject project) {
        File mainFile = getFileForProject(project);
        File mainFileDir = mainFile.getParentFile();

        return mainFileDir != null
                ? new File(mainFileDir, SETTINGS_DIR_NAME)
                : new File(SETTINGS_DIR_NAME);
    }

    private static File getProfileDirectory(NbGradleProject project) {
        return new File(getSettingsDir(project), PROFILE_DIRECTORY);
    }

    public static File[] getFilesForProfile(NbGradleProject project, String profile) {
        File mainFile = getFileForProject(project);

        if (profile == null) {
            return new File[]{mainFile};
        }
        else {
            File mainFileDir = mainFile.getParentFile();

            File profileFile = mainFileDir != null
                    ? new File(mainFileDir, SETTINGS_DIR_NAME)
                    : new File(SETTINGS_DIR_NAME);
            profileFile = new File(profileFile, PROFILE_DIRECTORY);
            profileFile = new File(profileFile, profile + PROFILE_FILE_NAME_SUFFIX);
            return new File[]{profileFile, mainFile};
        }
    }

    public static File[] getFilesForProject(NbGradleProject project) {
        return getFilesForProfile(project, project.getCurrentProfile().getProfileName());
    }

    private static File getFileForProject(NbGradleProject project) {
        NbGradleModel model = project.getAvailableModel();
        FileObject settingsFile = model.getSettingsFile();
        FileObject dir = settingsFile != null
                ? settingsFile.getParent()
                : project.getProjectDirectory();
        if (dir == null) {
            dir = project.getProjectDirectory();
        }

        File outputDir = FileUtil.toFile(dir);
        if (outputDir == null) {
            throw new IllegalArgumentException("Cannot get the properties file because the directory is missing: " + dir);
        }

        return new File(outputDir, SETTINGS_FILENAME);
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
