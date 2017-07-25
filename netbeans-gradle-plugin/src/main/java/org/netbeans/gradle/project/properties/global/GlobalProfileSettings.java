package org.netbeans.gradle.project.properties.global;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import org.netbeans.gradle.project.util.LazyValue;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbSupplier;
import org.w3c.dom.Element;

final class GlobalProfileSettings implements LoadableSingleProfileSettingsEx {
    private static final GlobalProfileSettings DEFAULT = new GlobalProfileSettings();

    private final GenericProfileSettings impl;

    private GlobalProfileSettings() {
        this.impl = new GenericProfileSettings(new GlobalProfileLocationProvider());
    }

    public static GlobalProfileSettings getInstance() {
        GlobalProfileSettings result = DEFAULT;
        LegacyUtils.moveLegacyConfig(result.impl);
        return result;
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
        private static final String BASE_FILE_NAME = ProfileKey.GLOBAL_PROFILE.getFileName() + ".xml";

        private final LazyValue<Path> outputPathRef;

        public GlobalProfileLocationProvider() {
            this.outputPathRef = new LazyValue<>(new NbSupplier<Path>() {
                @Override
                public Path get() {
                    return GlobalSettingsUtils.configRoot().tryGetSubPath(BASE_FILE_NAME);
                }
            });
        }

        @Override
        public ProfileKey getKey() {
            return ProfileKey.GLOBAL_PROFILE;
        }

        @Override
        public Path tryGetOutputPath() throws IOException {
            return outputPathRef.get();
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
        private static volatile boolean moveInProgress = false;
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
                if (!moved && !moveInProgress) {
                    try {
                        // moveInProgress prevents infinite recursion and possible cycles in initialization
                        moveInProgress = true;
                        moveToNewSettings(globalSettings);
                    } finally {
                        moveInProgress = false;
                    }
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
