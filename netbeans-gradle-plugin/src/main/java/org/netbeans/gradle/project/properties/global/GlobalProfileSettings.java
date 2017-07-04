package org.netbeans.gradle.project.properties.global;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.properties.ConfigSaveOptions;
import org.netbeans.gradle.project.properties.DomElementKey;
import org.netbeans.gradle.project.properties.GenericProfileSettings;
import org.netbeans.gradle.project.properties.LoadableSingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.ProfileFileDef;
import org.netbeans.gradle.project.properties.ProfileLocationProvider;
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
        return DEFAULT;
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
                    return GlobalSettingsUtils.tryGetGlobalConfigPath(BASE_FILE_NAME);
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
}
