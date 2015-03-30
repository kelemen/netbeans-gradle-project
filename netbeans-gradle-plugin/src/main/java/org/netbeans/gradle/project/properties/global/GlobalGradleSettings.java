package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationDirectory;
import org.netbeans.gradle.project.properties.JavaProjectPlatform;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
import org.netbeans.gradle.project.properties.StringBasedProperty;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

public final class GlobalGradleSettings {
    private static final Logger LOGGER = Logger.getLogger(GlobalGradleSettings.class.getName());

    private static final GlobalGradleSettings DEFAULT = new GlobalGradleSettings(null);
    private static volatile BasicPreference PREFERENCE = new DefaultPreference();

    private final StringBasedProperty<GradleLocationDef> gradleLocation;
    private final StringBasedProperty<File> gradleUserHomeDir;
    private final StringBasedProperty<List<String>> gradleArgs;
    private final StringBasedProperty<List<String>> gradleJvmArgs;
    private final StringBasedProperty<JavaPlatform> gradleJdk;
    private final StringBasedProperty<Boolean> skipTests;
    private final StringBasedProperty<Boolean> skipCheck;
    private final StringBasedProperty<Integer> projectCacheSize;
    private final StringBasedProperty<Boolean> alwaysClearOutput;
    private final StringBasedProperty<Boolean> omitInitScript;
    private final StringBasedProperty<Boolean> mayRelyOnJavaOfScript;
    private final StringBasedProperty<ModelLoadingStrategy> modelLoadingStrategy;
    private final StringBasedProperty<Integer> gradleDaemonTimeoutSec;
    private final StringBasedProperty<Boolean> compileOnSave;
    private final StringBasedProperty<PlatformOrder> platformPreferenceOrder;
    private final StringBasedProperty<String> displayNamePattern;

    public GlobalGradleSettings(String namespace) {
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
        omitInitScript = new GlobalProperty<>(
                withNS(namespace, "omit-init-script"),
                new BooleanConverter(false));
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
    }

    public static void setDefaultPreference() {
        PREFERENCE = new DefaultPreference();
    }

    // Testing only
    public static void setCleanMemoryPreference() {
        PREFERENCE = new MemPreference();
    }

    private static String withNS(String namespace, String name) {
        return namespace == null ? name : namespace + "." + name;
    }

    public void setAllToDefault() {
        for (Field field: getClass().getDeclaredFields()) {
            if (StringBasedProperty.class.isAssignableFrom(field.getType())) {
                try {
                    StringBasedProperty<?> property = (StringBasedProperty<?>)field.get(this);
                    property.setValueFromString(null);
                } catch (IllegalAccessException ex) {
                    throw new AssertionError(ex);
                }
            }
        }
    }

    public StringBasedProperty<Integer> gradleDaemonTimeoutSec() {
        return gradleDaemonTimeoutSec;
    }

    public StringBasedProperty<GradleLocationDef> gradleLocation() {
        return gradleLocation;
    }

    public StringBasedProperty<File> gradleUserHomeDir() {
        return gradleUserHomeDir;
    }

    public StringBasedProperty<List<String>> gradleJvmArgs() {
        return gradleJvmArgs;
    }

    public StringBasedProperty<List<String>> gradleArgs() {
        return gradleArgs;
    }

    public StringBasedProperty<JavaPlatform> gradleJdk() {
        return gradleJdk;
    }

    public StringBasedProperty<Boolean> skipTests() {
        return skipTests;
    }

    public StringBasedProperty<Boolean> skipCheck() {
        return skipCheck;
    }

    public StringBasedProperty<Integer> projectCacheSize() {
        return projectCacheSize;
    }

    public StringBasedProperty<Boolean> alwaysClearOutput() {
        return alwaysClearOutput;
    }

    public StringBasedProperty<Boolean> omitInitScript() {
        return omitInitScript;
    }

    public StringBasedProperty<Boolean> mayRelyOnJavaOfScript() {
        return mayRelyOnJavaOfScript;
    }

    public StringBasedProperty<ModelLoadingStrategy> modelLoadingStrategy() {
        return modelLoadingStrategy;
    }

    public StringBasedProperty<Boolean> compileOnSave() {
        return compileOnSave;
    }

    public StringBasedProperty<PlatformOrder> platformPreferenceOrder() {
        return platformPreferenceOrder;
    }

    public StringBasedProperty<String> displayNamePattern() {
        return displayNamePattern;
    }

    public static GlobalGradleSettings getDefault() {
        return DEFAULT;
    }

    public File getGradleInstallationAsFile() {
        GradleLocationDef locationDef = gradleLocation.getValue();
        GradleLocation location = locationDef.getLocation();
        if (location instanceof GradleLocationDirectory) {
            return ((GradleLocationDirectory)location).getGradleHome();
        }
        return null;
    }

    public FileObject getGradleLocation() {
        File result = getGradleInstallationAsFile();
        return result != null ? FileUtil.toFileObject(result) : null;
    }

    public List<JavaPlatform> filterIndistinguishable(JavaPlatform[] platforms) {
        return filterIndistinguishable(Arrays.asList(platforms));
    }

    public List<JavaPlatform> filterIndistinguishable(Collection<JavaPlatform> platforms) {
        List<JavaPlatform> result = new ArrayList<>(platforms.size());
        Set<NameAndVersion> foundVersions = CollectionUtils.newHashSet(platforms.size());

        for (JavaPlatform platform: orderPlatforms(platforms)) {
            if (foundVersions.add(new NameAndVersion(platform))) {
                result.add(platform);
            }
        }

        return result;
    }

    public List<JavaPlatform> orderPlatforms(JavaPlatform[] platforms) {
        return orderPlatforms(Arrays.asList(platforms));
    }

    public List<JavaPlatform> orderPlatforms(Collection<JavaPlatform> platforms) {
        PlatformOrder order = platformPreferenceOrder().getValue();
        return order.orderPlatforms(platforms);
    }

    public static FileObject getHomeFolder(JavaPlatform platform) {
        Collection<FileObject> installFolders = platform.getInstallFolders();
        int numberOfFolder = installFolders.size();
        if (numberOfFolder == 0) {
            LOGGER.log(Level.WARNING, "Selected platform contains no installation folders: {0}", platform.getDisplayName());
            return null;
        }

        if (numberOfFolder > 1) {
            LOGGER.log(Level.WARNING, "Selected platform contains multiple installation folders: {0}", platform.getDisplayName());
        }

        return installFolders.iterator().next();
    }

    public FileObject getCurrentGradleJdkHome() {
        JavaPlatform platform = gradleJdk.getValue();
        if (platform == null) {
            return null;
        }

        return getHomeFolder(platform);
    }

    public static List<String> stringToStringList(String strValue) {
        if (strValue == null || strValue.isEmpty()) {
            return null;
        }

        return Collections.unmodifiableList(Arrays.asList(StringUtils.splitLines(strValue)));
    }

    public static String stringListToString(Collection<String> value) {
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
                    ? PlatformOrder.fromStringFormat(strValue)
                    : PlatformOrder.DEFAULT_ORDER;
        }

        @Override
        public String toString(PlatformOrder value) {
            return value != null ? value.toStringFormat() : null;
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

        @SuppressWarnings("unchecked")
        public EnumConverter(EnumType defaultValue) {
            this.defaultValue = defaultValue;
            this.enumClass = (Class<EnumType>)defaultValue.getClass();
        }

        @Override
        public EnumType toValue(String strValue) {
            if (strValue == null || strValue.isEmpty()) {
                return defaultValue;
            }

            try {
                return Enum.valueOf(enumClass, strValue);
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.INFO,
                        "Illegal enum value for config: " + strValue + " expected an instance of " + enumClass.getSimpleName(),
                        ex);
                return defaultValue;
            }
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

    private static final class DefaultPreference implements BasicPreference {
        private static Preferences getPreferences() {
            return NbPreferences.forModule(NbGradleProjectFactory.class);
        }

        @Override
        public void put(String key, String value) {
            getPreferences().put(key, value);
        }

        @Override
        public void remove(String key) {
            getPreferences().remove(key);
        }

        @Override
        public String get(String key) {
            return getPreferences().get(key, null);
        }

        @Override
        public ListenerRef addPreferenceChangeListener(final PreferenceChangeListener pcl) {
            final Preferences preferences = getPreferences();

            // To be super safe, we could wrap the listener
            preferences.addPreferenceChangeListener(pcl);
            return NbListenerRefs.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    preferences.removePreferenceChangeListener(pcl);
                }
            });
        }
    }

    private static final class MemPreference implements BasicPreference {
        private final Map<String, String> values;
        private final ListenerManager<PreferenceChangeListener> listeners;

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

    private interface BasicPreference {
        public void put(String key, String value);
        public void remove(String key);
        public String get(String key);

        public ListenerRef addPreferenceChangeListener(PreferenceChangeListener pcl);
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

    private GlobalGradleSettings() {
        throw new AssertionError();
    }
}
