package org.netbeans.gradle.project.properties;

import java.io.File;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.gradle.project.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

public final class GlobalGradleSettings {
    private static final Logger LOGGER = Logger.getLogger(GlobalGradleSettings.class.getName());

    private static final StringBasedProperty<FileObject> GRADLE_HOME;
    private static final StringBasedProperty<List<String>> GRADLE_JVM_ARGS;
    private static final StringBasedProperty<JavaPlatform> GRADLE_JDK;
    private static final StringBasedProperty<Boolean> SKIP_TESTS;
    private static final StringBasedProperty<Integer> PROJECT_CACHE_SIZE;
    private static final StringBasedProperty<Boolean> ALWAYS_CLEAR_OUTPUT;

    static {
        GRADLE_HOME = new GlobalProperty<FileObject>("gradle-home", GradleHomeConverter.INSTANCE);
        GRADLE_JVM_ARGS = new GlobalProperty<List<String>>("gradle-jvm-args", StringToStringListConverter.INSTANCE);
        GRADLE_JDK = new GlobalProperty<JavaPlatform>("gradle-jdk", JavaPlaformConverter.INSTANCE);
        SKIP_TESTS = new GlobalProperty<Boolean>("skip-tests", new BooleanConverter(false));
        PROJECT_CACHE_SIZE = new GlobalProperty<Integer>("project-cache-size", new IntegerConverter(1, Integer.MAX_VALUE, 100));
        ALWAYS_CLEAR_OUTPUT = new GlobalProperty<Boolean>("always-clear-output", new BooleanConverter(false));
    }

    public static StringBasedProperty<FileObject> getGradleHome() {
        return GRADLE_HOME;
    }

    public static StringBasedProperty<List<String>> getGradleJvmArgs() {
        return GRADLE_JVM_ARGS;
    }

    public static StringBasedProperty<JavaPlatform> getGradleJdk() {
        return GRADLE_JDK;
    }

    public static StringBasedProperty<Boolean> getSkipTests() {
        return SKIP_TESTS;
    }

    public static StringBasedProperty<Integer> getProjectCacheSize() {
        return PROJECT_CACHE_SIZE;
    }

    public static StringBasedProperty<Boolean> getAlwaysClearOutput() {
        return ALWAYS_CLEAR_OUTPUT;
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
        JavaPlatform platform = GRADLE_JDK.getValue();
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

            while (!valueItr.hasNext()) {
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
            if (value != null && value.equals(defaultValue)) {
                return null;
            }
            // This check only matters in case value and defaultValue are both nulls.
            if (value == defaultValue) {
                return null;
            }

            return value != null ? value.toString() : null;
        }
    }

    private static class BooleanConverter implements ValueConverter<Boolean> {
        private static String TRUE_STR = Boolean.TRUE.toString();
        private static String FALSE_STR = Boolean.FALSE.toString();

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
            if (value != null && value.equals(defaultValue)) {
                return null;
            }
            // This check only matters in case value and defaultValue are both nulls.
            if (value == defaultValue) {
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

    private enum GradleHomeConverter implements ValueConverter<FileObject> {
        INSTANCE;

        @Override
        public FileObject toValue(String strValue) {
            String gradleHome;
            if (strValue == null || strValue.isEmpty())  {
                gradleHome = System.getenv("GRADLE_HOME");
                gradleHome = gradleHome != null ? gradleHome.trim() : "";
            }
            else {
                gradleHome = strValue;
            }
            if (gradleHome.isEmpty()) {
                return null;
            }

            return FileUtil.toFileObject(FileUtil.normalizeFile(new File(gradleHome)));
        }

        @Override
        public String toString(FileObject value) {
            if (value == null) {
                return null;
            }

            File fileValue = FileUtil.toFile(value);
            if (fileValue == null) {
                return null;
            }

            return fileValue.getPath();
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
            if (settingsName == null) throw new NullPointerException("settingsName");
            if (converter == null) throw new NullPointerException("converter");

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
        public void addChangeListener(ChangeListener listener) {
            getPreferences().addPreferenceChangeListener(
                    new ChangeListenerWrapper(settingsName, listener));
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            // We assume that listeners are looked up based on the equals
            // method. This is not documented to be necessary and for example:
            // AWTEventMulticaster does rely on reference equality. Still, we
            // hope for the best as there is nothing else we can do.
            getPreferences().removePreferenceChangeListener(
                    new ChangeListenerWrapper(settingsName, listener));
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

    private static class ChangeListenerWrapper implements PreferenceChangeListener {
        private final String preferenceName;
        private final ChangeListener wrapped;

        public ChangeListenerWrapper(String preferenceName, ChangeListener wrapped) {
            if (preferenceName == null) throw new NullPointerException("preferenceName");
            if (wrapped == null) throw new NullPointerException("wrapped");

            this.preferenceName = preferenceName;
            this.wrapped = wrapped;
        }

        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
            if (preferenceName.equals(evt.getKey())) {
                wrapped.stateChanged(new ChangeEvent(evt.getSource()));
            }
        }

        @Override
        public int hashCode() {
            return 61 * wrapped.hashCode() + 3;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ChangeListenerWrapper other = (ChangeListenerWrapper)obj;
            return this.wrapped.equals(other.wrapped);
        }
    }

    private GlobalGradleSettings() {
        throw new AssertionError();
    }
}
