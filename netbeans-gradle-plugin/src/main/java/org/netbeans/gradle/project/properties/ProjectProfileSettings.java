package org.netbeans.gradle.project.properties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Element;

final class ProjectProfileSettings implements LoadableSingleProfileSettingsEx {
    private static final Logger LOGGER = Logger.getLogger(ProjectProfileSettings.class.getName());

    private final GenericProfileSettings impl;

    public ProjectProfileSettings(ProjectProfileSettingsKey key) {
        Objects.requireNonNull(key, "key");

        this.impl = new GenericProfileSettings(new ProjectProfileLocationProvider(key));
    }

    @Override
    public Collection<DomElementKey> getAuxConfigKeys() {
        return impl.getAuxConfigKeys();
    }

    @Override
    public Element getAuxConfigValue(DomElementKey key) {
        return impl.getAuxConfigValue(key);
    }

    @Override
    public boolean setAuxConfigValue(DomElementKey key, Element value) {
        return impl.setAuxConfigValue(key, value);
    }

    @Override
    public void saveAndWait() {
        impl.saveAndWait();
    }

    @Override
    public ProfileKey getKey() {
        return impl.getKey();
    }

    @Override
    public <ValueType> MutableProperty<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef) {
        return impl.getProperty(propertyDef);
    }

    @Override
    public void ensureLoadedAndWait() {
        impl.ensureLoadedAndWait();
    }

    @Override
    public void ensureLoaded() {
        impl.ensureLoaded();
    }

    @Override
    public ListenerRef notifyWhenLoaded(Runnable runnable) {
        return impl.notifyWhenLoaded(runnable);
    }

    private static final class ProjectProfileLocationProvider implements ProfileLocationProvider {
        private final ProjectProfileSettingsKey key;

        public ProjectProfileLocationProvider(ProjectProfileSettingsKey key) {
            this.key = key;
        }

        @Override
        public ProfileKey getKey() {
            return key.getKey();
        }

        @Override
        public Path tryGetOutputPath() throws IOException {
            return SettingsFiles.getProfileFile(key.getProjectDir(), key.getKey());
        }

        @Override
        public ProfileFileDef tryGetOutputDef() throws IOException {
            Project project = tryGetProject();
            if (project == null) {
                LOGGER.log(Level.WARNING, "No project in {0}", key.getProjectDir());
                return null;
            }

            Path profileFile = tryGetOutputPath();
            if (profileFile == null) {
                return null;
            }

            ConfigSaveOptions saveOptions = ConfigXmlUtils.getSaveOptions(project, profileFile);
            return new ProfileFileDef(profileFile, saveOptions);
        }

        private Project tryGetProject() throws IOException {
            Path projectDir = key.getProjectDir();
            FileObject projectDirObj = FileUtil.toFileObject(projectDir.toFile());

            return projectDirObj != null
                    ? ProjectManager.getDefault().findProject(projectDirObj)
                    : null;
        }
    }
}
