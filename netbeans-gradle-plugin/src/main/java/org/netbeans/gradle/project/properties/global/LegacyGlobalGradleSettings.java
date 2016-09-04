package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.JavaProjectPlatform;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;
import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;

/**
 * @deprecated Use {@link CommonGlobalSettings} instead.
 */
@Deprecated
final class LegacyGlobalGradleSettings {
    private static final Logger LOGGER = Logger.getLogger(LegacyGlobalGradleSettings.class.getName());

    private static final LegacyGlobalGradleSettings DEFAULT = new LegacyGlobalGradleSettings(null);
    private static final BasicPreference PREFERENCE = NbGlobalPreference.DEFAULT;

    private final StringBasedProperty<GradleLocationDef> gradleLocation;
    private final StringBasedProperty<File> gradleUserHomeDir;
    private final StringBasedProperty<List<String>> gradleArgs;
    private final StringBasedProperty<List<String>> gradleJvmArgs;
    private final StringBasedProperty<JavaPlatform> gradleJdk;
    private final StringBasedProperty<Boolean> skipTests;
    private final StringBasedProperty<Boolean> skipCheck;
    private final StringBasedProperty<Integer> projectCacheSize;
    private final StringBasedProperty<Boolean> alwaysClearOutput;
    private final StringBasedProperty<SelfMaintainedTasks> selfMaintainedTasks;
    private final StringBasedProperty<Boolean> mayRelyOnJavaOfScript;
    private final StringBasedProperty<ModelLoadingStrategy> modelLoadingStrategy;
    private final StringBasedProperty<Integer> gradleDaemonTimeoutSec;
    private final StringBasedProperty<Boolean> compileOnSave;
    private final StringBasedProperty<PlatformOrder> platformPreferenceOrder;
    private final StringBasedProperty<String> displayNamePattern;
    private final StringBasedProperty<JavaSourcesDisplayMode> javaSourcesDisplayMode;
    private final StringBasedProperty<Boolean> replaceLfOnStdIn;
    private final StringBasedProperty<DebugMode> debugMode;
    private final StringBasedProperty<Boolean> loadRootProjectFirst;
    private final StringBasedProperty<Boolean> detectProjectDependenciesByJarName;

    public LegacyGlobalGradleSettings(String namespace) {
        // "gradle-home" is probably not the best name but it must remain so
        // for backward compatibility reason.
        gradleLocation = new GlobalProperty<>(
                withNS(namespace, "gradle-home"),
                GradleLocationConverter.INSTANCE);
        gradleUserHomeDir = new GlobalProperty<>(
                withNS(namespace, "gradle-user-home"),
                FileConverter.INSTANCE);
        gradleArgs = new GlobalProperty<>(
                withNS(namespace, "gradle-args"),
                StringToStringListConverter.INSTANCE);
        gradleJvmArgs = new GlobalProperty<>(
                withNS(namespace, "gradle-jvm-args"),
                StringToStringListConverter.INSTANCE);
        gradleJdk = new GlobalProperty<>(
                withNS(namespace, "gradle-jdk"),
                JavaPlaformConverter.INSTANCE);
        skipTests = new GlobalProperty<>(
                withNS(namespace, "skip-tests"),
                new BooleanConverter(false));
        skipCheck = new GlobalProperty<>(
                withNS(namespace, "skip-check"),
                new BooleanConverter(false));
        projectCacheSize = new GlobalProperty<>(
                withNS(namespace, "project-cache-size"),
                new IntegerConverter(1, Integer.MAX_VALUE, 100));
        alwaysClearOutput = new GlobalProperty<>(
                withNS(namespace, "always-clear-output"),
                new BooleanConverter(false));
        selfMaintainedTasks = new GlobalProperty<>(
                withNS(namespace, "omit-init-script"),
                new EnumConverter<>(SelfMaintainedTasks.FALSE));
        mayRelyOnJavaOfScript = new GlobalProperty<>(
                withNS(namespace, "rely-on-java-of-script"),
                new BooleanConverter(false));
        modelLoadingStrategy = new GlobalProperty<>(
                withNS(namespace, "model-load-strategy"),
                new EnumConverter<>(ModelLoadingStrategy.NEWEST_POSSIBLE));
        gradleDaemonTimeoutSec = new GlobalProperty<>(
                withNS(namespace, "gradle-daemon-timeout-sec"),
                new IntegerConverter(1, Integer.MAX_VALUE, null));
        compileOnSave = new GlobalProperty<>(
                withNS(namespace, "compile-on-save"),
                new BooleanConverter(false));
        platformPreferenceOrder = new GlobalProperty<>(
                withNS(namespace, "platform-pref-order"),
                PlatformOrderConverter.INSTANCE
        );
        displayNamePattern = new GlobalProperty<>(
                withNS(namespace, "display-name-pattern"),
                new StringConverter(DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant())
        );
        javaSourcesDisplayMode = new GlobalProperty<>(
                withNS(namespace, "java-sources-display-mode"),
                new EnumConverter<>(JavaSourcesDisplayMode.DEFAULT_MODE));
        replaceLfOnStdIn = new GlobalProperty<>(
                withNS(namespace, "replace-lf-on-stdin"),
                new BooleanConverter(true));
        debugMode = new GlobalProperty<>(
                withNS(namespace, "debug-mode"),
                new EnumConverter<>(DebugMode.DEBUGGER_ATTACHES));
        loadRootProjectFirst = new GlobalProperty<>(
                withNS(namespace, "load-root-first"),
                new BooleanConverter(true));
        detectProjectDependenciesByJarName = new GlobalProperty<>(
                withNS(namespace, "detect-project-dep-by-jar-name"),
                new BooleanConverter(false));
    }

    private <T> void moveToNewSettings(
            StringBasedProperty<? extends T> oldProperty,
            PropertyReference<? super T> newProperty) {
        if (oldProperty.getValueAsString() != null) {
            newProperty.setValue(oldProperty.getValue());
            oldProperty.setValueFromString(null);
        }
    }

    public static void moveDefaultToNewSettings(CommonGlobalSettings newSettings) {
        DEFAULT.moveToNewSettings(newSettings);
    }

    private void moveToNewSettings(CommonGlobalSettings newSettings) {
        moveToNewSettings(gradleLocation, newSettings.gradleLocation());
        moveToNewSettings(gradleUserHomeDir, newSettings.gradleUserHomeDir());
        moveToNewSettings(gradleArgs, newSettings.gradleArgs());
        moveToNewSettings(gradleJvmArgs, newSettings.gradleJvmArgs());
        moveToNewSettings(gradleJdk, newSettings.defaultJdk());
        moveToNewSettings(skipTests, newSettings.skipTests());
        moveToNewSettings(skipCheck, newSettings.skipCheck());
        moveToNewSettings(projectCacheSize, newSettings.projectCacheSize());
        moveToNewSettings(alwaysClearOutput, newSettings.alwaysClearOutput());
        moveToNewSettings(selfMaintainedTasks, newSettings.selfMaintainedTasks());
        moveToNewSettings(mayRelyOnJavaOfScript, newSettings.mayRelyOnJavaOfScript());
        moveToNewSettings(modelLoadingStrategy, newSettings.modelLoadingStrategy());
        moveToNewSettings(gradleDaemonTimeoutSec, newSettings.gradleDaemonTimeoutSec());
        moveToNewSettings(compileOnSave, newSettings.compileOnSave());
        moveToNewSettings(platformPreferenceOrder, newSettings.platformPreferenceOrder());
        moveToNewSettings(displayNamePattern, newSettings.displayNamePattern());
        moveToNewSettings(javaSourcesDisplayMode, newSettings.javaSourcesDisplayMode());
        moveToNewSettings(replaceLfOnStdIn, newSettings.replaceLfOnStdIn());
        moveToNewSettings(debugMode, newSettings.debugMode());
        moveToNewSettings(loadRootProjectFirst, newSettings.loadRootProjectFirst());
        moveToNewSettings(detectProjectDependenciesByJarName, newSettings.detectProjectDependenciesByJarName());
    }

    private static String withNS(String namespace, String name) {
        return namespace == null ? name : namespace + "." + name;
    }

    MutableProperty<List<String>> gradleJvmArgs() {
        return gradleJvmArgs;
    }

    private static List<String> stringToStringList(String strValue) {
        if (strValue == null || strValue.isEmpty()) {
            return null;
        }

        return Collections.unmodifiableList(Arrays.asList(StringUtils.splitLines(strValue)));
    }

    private static String stringListToString(Collection<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        int length = value.size() - 1;
        for (String line: value) {
            length += line.length();
        }

        StringBuilder result = new StringBuilder(length);
        Iterator<String> valueItr = value.iterator();
        // valueItr.next() should succeed since the list is not empty.
        result.append(valueItr.next());

        while (valueItr.hasNext()) {
            result.append('\n');
            result.append(valueItr.next());
        }
        return result.toString();
    }

    private enum PlatformOrderConverter implements ValueConverter<PlatformOrder> {
        INSTANCE;

        @Override
        public PlatformOrder toValue(String strValue) {
            return strValue != null
                    ? fromStringFormat(strValue)
                    : PlatformOrder.DEFAULT_ORDER;
        }

        @Override
        public String toString(PlatformOrder value) {
            return value != null ? toStringFormat(value) : null;
        }


        public static PlatformOrder fromStringFormat(String strValue) {
            List<String> platformIds = stringToStringList(strValue);
            if (platformIds == null) {
                return null;
            }
            return PlatformOrder.fromPlatformIds(platformIds);
        }

        public String toStringFormat(PlatformOrder order) {
            return stringListToString(order.getPlatformIds());
        }
    }

    private enum StringToStringListConverter implements ValueConverter<List<String>> {
        INSTANCE;

        @Override
        public List<String> toValue(String strValue) {
            return stringToStringList(strValue);
        }

        @Override
        public String toString(List<String> value) {
            return stringListToString(value);
        }
    }

    private static class IntegerConverter implements ValueConverter<Integer> {
        private final Integer defaultValue;
        private final int minValue;
        private final int maxValue;

        public IntegerConverter(int minValue, int maxValue, Integer defaultValue) {
            if (minValue > maxValue) {
                throw new IllegalArgumentException("minValue > maxValue");
            }
            if (defaultValue != null && (minValue > defaultValue || maxValue < defaultValue)) {
                throw new IllegalArgumentException("minValue > defaultValue || maxValue < defaultValue");
            }
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.defaultValue = defaultValue;
        }

        @Override
        public Integer toValue(String strValue) {
            if (strValue == null || strValue.isEmpty()) {
                return defaultValue;
            }

            try {
                int result = Integer.parseInt(strValue);
                if (result < minValue) result = minValue;
                else if (result > maxValue) result = maxValue;
                return result;
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid integer in the settings: {0}", strValue);
            }
            return defaultValue;
        }

        @Override
        public String toString(Integer value) {
            if (Utilities.compareObjects(value, defaultValue)) {
                return null;
            }

            return value != null ? value.toString() : null;
        }
    }

    private static class EnumConverter<EnumType extends Enum<EnumType>>
    implements
            ValueConverter<EnumType> {

        private final EnumType defaultValue;
        private final Class<EnumType> enumClass;
        private final Map<String, EnumType> byNameValues;

        @SuppressWarnings("unchecked")
        public EnumConverter(EnumType defaultValue) {
            this.defaultValue = defaultValue;
            this.enumClass = (Class<EnumType>)defaultValue.getClass();

            EnumType[] allValues = enumClass.getEnumConstants();
            byNameValues = CollectionsEx.newHashMap(allValues.length);
            for (EnumType value: allValues) {
                byNameValues.put(value.name().toUpperCase(Locale.ROOT), value);
            }
        }

        @Override
        public EnumType toValue(String strValue) {
            if (strValue == null || strValue.isEmpty()) {
                return defaultValue;
            }

            EnumType result = byNameValues.get(strValue.toUpperCase(Locale.ROOT));
            if (result == null) {
                LOGGER.log(Level.INFO,
                        "Illegal enum value for config: {0} expected an instance of {1}",
                        new Object[]{strValue, enumClass.getSimpleName()});
                result = defaultValue;
            }
            return result;
        }

        @Override
        public String toString(EnumType value) {
            if (value == defaultValue || value == null) {
                return null;
            }
            return value.name();
        }
    }

    private static class BooleanConverter implements ValueConverter<Boolean> {
        private static final String TRUE_STR = Boolean.TRUE.toString();
        private static final String FALSE_STR = Boolean.FALSE.toString();

        private final Boolean defaultValue;

        public BooleanConverter(Boolean defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Boolean toValue(String strValue) {
            if (strValue == null || strValue.isEmpty()) {
                return defaultValue;
            }

            if (TRUE_STR.equals(strValue)) return true;
            if (FALSE_STR.equals(strValue)) return false;
            return defaultValue;
        }

        @Override
        public String toString(Boolean value) {
            if (Objects.equals(value, defaultValue)) {
                return null;
            }

            return value != null ? value.toString() : null;
        }
    }

    private enum JavaPlaformConverter implements ValueConverter<JavaPlatform> {
        INSTANCE;

        @Override
        public JavaPlatform toValue(String strValue) {
            if (strValue == null || strValue.isEmpty()) {
                return JavaPlatform.getDefault();
            }

            JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
            for (JavaPlatform platform: platforms) {
                if (strValue.equals(toString(platform))) {
                    return platform;
                }
            }
            return JavaPlatform.getDefault();
        }

        @Override
        public String toString(JavaPlatform value) {
            if (value == null) {
                return null;
            }

            StringBuilder result = new StringBuilder(1024);
            for (FileObject installFolder: value.getInstallFolders()) {
                String path = installFolder.getPath();
                if (result.length() > 0) {
                    result.append(";");
                }
                result.append(path);
            }
            return result.toString();
        }
    }

    private enum FileConverter implements ValueConverter<File> {
        INSTANCE;

        @Override
        public File toValue(String strValue) {
            if (strValue == null || strValue.isEmpty())  {
                return null;
            }
            return new File(strValue);
        }

        @Override
        public String toString(File value) {
            return value != null ? value.getPath() : null;
        }
    }

    private static final class StringConverter implements ValueConverter<String> {
        private final String defaultValue;

        public StringConverter(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String toValue(String strValue) {
            return strValue != null ? strValue : defaultValue;
        }

        @Override
        public String toString(String value) {
            return Objects.equals(value, defaultValue) ? null : value;
        }
    }

    private enum GradleLocationConverter implements ValueConverter<GradleLocationDef> {
        INSTANCE;

        @Override
        public GradleLocationDef toValue(String strValue) {
            if (strValue == null)  {
                return GradleLocationDef.DEFAULT;
            }
            return GradleLocationDef.parseFromString(strValue);
        }

        @Override
        public String toString(GradleLocationDef value) {
            if (value == null) {
                return null;
            }

            if (Objects.equals(value, GradleLocationDef.DEFAULT)) {
                return null;
            }
            else {
                return value.toStringFormat();
            }
        }
    }

    private static interface ValueConverter<ValueType> {
        public ValueType toValue(String strValue);
        public String toString(ValueType value);
    }

    private static class GlobalProperty<ValueType> implements StringBasedProperty<ValueType> {
        private final String settingsName;
        private final ValueConverter<ValueType> converter;

        public GlobalProperty(String settingsName, ValueConverter<ValueType> converter) {
            ExceptionHelper.checkNotNullArgument(settingsName, "settingsName");
            ExceptionHelper.checkNotNullArgument(converter, "converter");

            this.settingsName = settingsName;
            this.converter = converter;
        }

        @Override
        public void setValue(ValueType value) {
            String strValue = converter.toString(value);
            setValueFromString(strValue);
        }

        @Override
        public ValueType getValue() {
            return converter.toValue(getValueAsString());
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            return PREFERENCE.addPreferenceChangeListener(new PreferenceChangeListener() {
                @Override
                public void preferenceChange(PreferenceChangeEvent evt) {
                    if (GlobalProperty.this.settingsName.equals(evt.getKey())) {
                        listener.run();
                    }
                }
            });
        }

        @Override
        public void setValueFromString(String strValue) {
            if (strValue != null) {
                PREFERENCE.put(settingsName, strValue);
            }
            else {
                PREFERENCE.remove(settingsName);
            }
        }

        @Override
        public String getValueAsString() {
            return PREFERENCE.get(settingsName);
        }
    }

    private static final class MemPreference implements BasicPreference, PreferenceContainer {
        final Map<String, String> values;
        final ListenerManager<PreferenceChangeListener> listeners;

        public MemPreference() {
            this.values = new ConcurrentHashMap<>();
            this.listeners = new CopyOnTriggerListenerManager<>();
        }

        private void fireChangeListener(String key, String newValue) {
            PreferenceChangeEvent evt = new PreferenceChangeEvent(Preferences.systemRoot(), key, newValue);

            // In Java 8, it can be PreferenceChangeListener::preferenceChange
            listeners.onEvent(new EventDispatcher<PreferenceChangeListener, PreferenceChangeEvent>() {
                @Override
                public void onEvent(PreferenceChangeListener eventListener, PreferenceChangeEvent arg) {
                    eventListener.preferenceChange(arg);
                }
            }, evt);
        }

        @Override
        public Map<String, String> getKeyValues(String namespace) {
            ExceptionHelper.checkNotNullArgument(namespace, "namespace");

            String prefix = namespace + ".";
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, String> entry: values.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    String rawKey = entry.getKey().substring(prefix.length());
                    result.put(rawKey, entry.getValue());
                }
            }
            return result;
        }

        @Override
        public void put(String key, String value) {
            if (value != null) {
                values.put(key, value);
                fireChangeListener(key, value);
            }
            else {
                remove(key);
            }
        }

        @Override
        public void remove(String key) {
            values.remove(key);
            fireChangeListener(key, null);
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public ListenerRef addPreferenceChangeListener(PreferenceChangeListener pcl) {
            return listeners.registerListener(pcl);
        }
    }

    public interface PreferenceContainer {
        public Map<String, String> getKeyValues(String namespace);
    }

    private static final class NameAndVersion {
        private final String name;
        private final String version;

        public NameAndVersion(JavaPlatform platform) {
            JavaProjectPlatform projectPlatform = new JavaProjectPlatform(platform);
            this.name = projectPlatform.getName();
            this.version = projectPlatform.getVersion();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + Objects.hashCode(this.version);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final NameAndVersion other = (NameAndVersion)obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.version, other.version);
        }
    }

    private interface StringBasedProperty<ValueType> extends MutableProperty<ValueType> {
        public void setValueFromString(String strValue);
        public String getValueAsString();
    }

    private LegacyGlobalGradleSettings() {
        throw new AssertionError();
    }
}
