package org.netbeans.gradle.project.properties;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

public final class GlobalGradleSettings {
    private static final Logger LOGGER = Logger.getLogger(GlobalGradleSettings.class.getName());

    private static final GlobalGradleSettings DEFAULT = new GlobalGradleSettings(null);

    private final StringBasedProperty<GradleLocation> gradleLocation;
    private final StringBasedProperty<File> gradleUserHomeDir;
    private final StringBasedProperty<List<String>> gradleJvmArgs;
    private final StringBasedProperty<JavaPlatform> gradleJdk;
    private final StringBasedProperty<Boolean> skipTests;
    private final StringBasedProperty<Integer> projectCacheSize;
    private final StringBasedProperty<Boolean> alwaysClearOutput;
    private final StringBasedProperty<Boolean> omitInitScript;
    private final StringBasedProperty<Boolean> mayRelyOnJavaOfScript;
    private final StringBasedProperty<ModelLoadingStrategy> modelLoadingStrategy;
    private final StringBasedProperty<Integer> gradleDaemonTimeoutSec;

    public GlobalGradleSettings(String namespace) {
        // "gradle-home" is probably not the best name but it must remain so
        // for backward compatibility reason.
        gradleLocation = new GlobalProperty<>(
                withNS(namespace, "gradle-home"),
                GradleLocationConverter.INSTANCE);
        gradleUserHomeDir = new GlobalProperty<>(
                withNS(namespace, "gradle-user-home"),
                FileConverter.INSTANCE);
        gradleJvmArgs = new GlobalProperty<>(
                withNS(namespace, "gradle-jvm-args"),
                StringToStringListConverter.INSTANCE);
        gradleJdk = new GlobalProperty<>(
                withNS(namespace, "gradle-jdk"),
                JavaPlaformConverter.INSTANCE);
        skipTests = new GlobalProperty<>(
                withNS(namespace, "skip-tests"),
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

    public StringBasedProperty<GradleLocation> gradleLocation() {
        return gradleLocation;
    }

    public StringBasedProperty<File> gradleUserHomeDir() {
        return gradleUserHomeDir;
    }

    public StringBasedProperty<List<String>> gradleJvmArgs() {
        return gradleJvmArgs;
    }

    public StringBasedProperty<JavaPlatform> gradleJdk() {
        return gradleJdk;
    }

    public StringBasedProperty<Boolean> skipTests() {
        return skipTests;
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

    public static GlobalGradleSettings getDefault() {
        return DEFAULT;
    }

    public static File getGradleInstallationAsFile() {
        GradleLocation location = getDefault().gradleLocation.getValue();
        if (location instanceof GradleLocationDirectory) {
            return ((GradleLocationDirectory)location).getGradleHome();
        }
        return null;
    }

    public static FileObject getGradleLocation() {
        File result = getGradleInstallationAsFile();
        return result != null ? FileUtil.toFileObject(result) : null;
    }

    public static StringBasedProperty<Integer> getGradleDaemonTimeoutSec() {
        return getDefault().gradleDaemonTimeoutSec;
    }

    public static StringBasedProperty<ModelLoadingStrategy> getModelLoadingStrategy() {
        return getDefault().modelLoadingStrategy;
    }

    public static StringBasedProperty<File> getGradleUserHomeDir() {
        return getDefault().gradleUserHomeDir;
    }

    public static StringBasedProperty<GradleLocation> getGradleHome() {
        return getDefault().gradleLocation;
    }

    public static StringBasedProperty<List<String>> getGradleJvmArgs() {
        return getDefault().gradleJvmArgs;
    }

    public static StringBasedProperty<JavaPlatform> getGradleJdk() {
        return getDefault().gradleJdk;
    }

    public static StringBasedProperty<Boolean> getSkipTests() {
        return getDefault().skipTests;
    }

    public static StringBasedProperty<Integer> getProjectCacheSize() {
        return getDefault().projectCacheSize;
    }

    public static StringBasedProperty<Boolean> getAlwaysClearOutput() {
        return getDefault().alwaysClearOutput;
    }

    public static StringBasedProperty<Boolean> getOmitInitScript() {
        return getDefault().omitInitScript;
    }

    public static StringBasedProperty<Boolean> getMayRelyOnJavaOfScript() {
        return getDefault().mayRelyOnJavaOfScript;
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

    public static FileObject getCurrentGradleJdkHome() {
        JavaPlatform platform = getDefault().gradleJdk.getValue();
        if (platform == null) {
            return null;
        }

        return getHomeFolder(platform);
    }

    private enum StringToStringListConverter implements ValueConverter<List<String>> {
        INSTANCE;

        @Override
        public List<String> toValue(String strValue) {
            if (strValue == null || strValue.isEmpty()) {
                return null;
            }

            return Collections.unmodifiableList(Arrays.asList(StringUtils.splitLines(strValue)));
        }

        @Override
        public String toString(List<String> value) {
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
            if (Utilities.compareObjects(value, defaultValue)) {
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

    private enum GradleLocationConverter implements ValueConverter<GradleLocation> {
        INSTANCE;

        @Override
        public GradleLocation toValue(String strValue) {
            if (strValue == null)  {
                return GradleLocationDefault.INSTANCE;
            }
            return AbstractProjectProperties.getGradleLocationFromString(strValue);
        }

        @Override
        public String toString(GradleLocation value) {
            if (value == null) {
                return null;
            }

            String result = AbstractProjectProperties.gradleLocationToString(value);
            return result.isEmpty() ? null : result;
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

        private static Preferences getPreferences() {
            // Use GradleSettingsPanel.class for compatibility.
            return NbPreferences.forModule(GradleSettingsPanel.class);
        }

        @Override
        public void setValueFromSource(PropertySource<? extends ValueType> source) {
            // Currently we ignore if the value of "source" changes for global
            // properties. This might need to be considered in the future.
            setValue(source.getValue());
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
        public boolean isDefault() {
            String strValue = getValueAsString();
            return strValue == null || strValue.isEmpty();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            final Preferences preferences = getPreferences();
            final PreferenceChangeListener changeListener = new PreferenceChangeListener() {
                @Override
                public void preferenceChange(PreferenceChangeEvent evt) {
                    if (GlobalProperty.this.settingsName.equals(evt.getKey())) {
                        listener.run();
                    }
                }
            };

            return NbListenerRefs.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    preferences.removePreferenceChangeListener(changeListener);
                }
            });
        }

        @Override
        public void setValueFromString(String strValue) {
            if (strValue != null) {
                getPreferences().put(settingsName, strValue);
            }
            else {
                getPreferences().remove(settingsName);
            }
        }

        @Override
        public String getValueAsString() {
            return getPreferences().get(settingsName, null);
        }
    }

    private GlobalGradleSettings() {
        throw new AssertionError();
    }
}
