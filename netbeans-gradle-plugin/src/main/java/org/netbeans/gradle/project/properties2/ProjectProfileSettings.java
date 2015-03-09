package org.netbeans.gradle.project.properties2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.property.MutableProperty;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.DomElementKey;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Element;

public final class ProjectProfileSettings {
    private static final Logger LOGGER = Logger.getLogger(ProjectProfileSettings.class.getName());

    private final ProfileSettingsKey key;
    private final ProfileSettings settings;

    private volatile boolean loaded;
    private final Lock loadLock;

    public ProjectProfileSettings(ProfileSettingsKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        this.key = key;
        this.settings = new ProfileSettings();
        this.loadLock = new ReentrantLock();
        this.loaded = false;
    }

    public ProfileSettingsKey getKey() {
        return key;
    }

    public static boolean isEventThread() {
        return ProfileSettings.isEventThread();
    }

    private Path tryGetProfileFile() throws IOException {
        Project project = tryGetProject();
        return project != null ? tryGetProfileFile(project) : null;
    }

    private Project tryGetProject() throws IOException {
        Path projectDir = key.getProjectDir();
        FileObject projectDirObj = FileUtil.toFileObject(projectDir.toFile());

        return ProjectManager.getDefault().findProject(projectDirObj);
    }

    private Path tryGetProfileFile(Project project) {
        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            LOGGER.log(Level.WARNING, "Not a Gradle project: {0}", project.getProjectDirectory());
            return null;
        }

        return SettingsFiles.getProfileFile(gradleProject, key.getKey()).toPath();
    }

    public void ensureLoaded() throws IOException {
        if (loaded) {
            return;
        }

        loadLock.lock();
        try {
            if (!loaded) {
                load();
            }
        } finally {
            loadLock.unlock();
        }
    }

    public void load() throws IOException {
        try {
            Path profileFile = tryGetProfileFile();
            if (profileFile == null) {
                LOGGER.log(Level.WARNING, "Cannot find location to save the profile: {0}", key);
                return;
            }

            settings.loadFromFile(profileFile);
        } finally {
            loaded = true;
        }
    }

    public void save() throws IOException {
        Project project = tryGetProject();
        if (project == null) {
            LOGGER.log(Level.WARNING, "No project in {0}", key.getProjectDir());
            return;
        }

        Path profileFile = tryGetProfileFile(project);
        if (profileFile == null) {
            return;
        }

        settings.saveToFile(project, profileFile);
    }

    public Element getAuxConfigValue(DomElementKey key) {
        return settings.getAuxConfigValue(key);
    }

    public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(ConfigPath configPath, PropertyDef<ValueKey, ValueType> propertyDef) {
        return settings.getProperty(configPath, propertyDef);
    }

    public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(Collection<ConfigPath> configPaths, PropertyDef<ValueKey, ValueType> propertyDef) {
        return settings.getProperty(configPaths, propertyDef);
    }
}
