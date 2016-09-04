package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.properties.ConfigSaveOptions;
import org.netbeans.gradle.project.properties.DomElementKey;
import org.netbeans.gradle.project.properties.GenericProfileSettings;
import org.netbeans.gradle.project.properties.LoadableSingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.MultiProfileProperties;
import org.netbeans.gradle.project.properties.ProfileFileDef;
import org.netbeans.gradle.project.properties.ProfileLocationProvider;
import org.netbeans.gradle.project.properties.SingleProfileSettingsEx;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Element;

final class GlobalProfileSettings implements LoadableSingleProfileSettingsEx {
    private static final Logger LOGGER = Logger.getLogger(GlobalProfileSettings.class.getName());

    private final GenericProfileSettings impl;

    public GlobalProfileSettings() {
        this.impl = new GenericProfileSettings(new GlobalProfileLocationProvider());
        LegacyUtils.moveLegacyConfig(this.impl);
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

    private static final class GlobalProfileLocationProvider implements ProfileLocationProvider {
        public GlobalProfileLocationProvider() {
        }

        @Override
        public ProfileKey getKey() {
            return ProfileKey.GLOBAL_PROFILE;
        }

        private FileObject getConfigRoot() throws IOException {
            return FileUtil.createFolder(FileUtil.getConfigRoot(), "Preferences");
        }

        private Path tryGetRootPath() throws IOException {
            File rootFile = FileUtil.toFile(getConfigRoot());
            if (rootFile == null) {
                LOGGER.log(Level.WARNING, "Unable to get config root folder.");
                return null;
            }

            return rootFile.toPath().resolve("org").resolve("netbeans").resolve("gradle");
        }

        @Override
        public Path tryGetOutputPath() throws IOException {
            Path root = tryGetRootPath();
            if (root == null) {
                return null;
            }

            return root.resolve("project-defaults.xml");
        }

        @Override
        public ProfileFileDef tryGetOutputDef() throws IOException {
            Path output = tryGetOutputPath();
            if (output == null) {
                return null;
            }

            ConfigSaveOptions saveOptions = getSaveOptions(output);

            return new ProfileFileDef(output, saveOptions);
        }

        private static ConfigSaveOptions getSaveOptions(Path output) {
            String lineSeparator = NbFileUtils.tryGetLineSeparatorForTextFile(output);
            return new ConfigSaveOptions(lineSeparator);
        }
    }

    private static class LegacyUtils {
        private static final Lock MOVE_LOCK = new ReentrantLock();
        private static volatile boolean moved = false;

        public static void moveLegacyConfig(GenericProfileSettings settings) {
            if (moved) {
                return;
            }

            String movedToNewConfig = NbGlobalPreference.DEFAULT.get("movedToNewConfig");
            if ("true".equalsIgnoreCase(movedToNewConfig)) {
                MOVE_LOCK.lock();
                try {
                    moved = true;
                } finally {
                    MOVE_LOCK.unlock();
                }
                return;
            }

            settings.ensureLoadedAndWait();

            MultiProfileProperties activeSettings
                    = new MultiProfileProperties(Collections.<SingleProfileSettingsEx>singletonList(settings));
            CommonGlobalSettings globalSettings = new CommonGlobalSettings(activeSettings);

            MOVE_LOCK.lock();
            try {
                if (!moved) {
                    moveToNewSettings(globalSettings);
                    moved = true;
                }
            } finally {
                MOVE_LOCK.unlock();
            }

            NbGlobalPreference.DEFAULT.put("movedToNewConfig", "true");
        }

        @SuppressWarnings("deprecation")
        private static void moveToNewSettings(CommonGlobalSettings globalSettings) {
            LegacyGlobalGradleSettings.moveDefaultToNewSettings(globalSettings);
        }
    }
}
