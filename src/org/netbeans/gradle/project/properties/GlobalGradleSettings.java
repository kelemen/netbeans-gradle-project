package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

public final class GlobalGradleSettings {
    private static final MutableProperty<FileObject> GRADLE_HOME;

    static {
        GRADLE_HOME = new GlobalProperty<FileObject>("gradle-home", GradleHomeConverter.INSTANCE);
    }

    public static MutableProperty<FileObject> getGradleHome() {
        return GRADLE_HOME;
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

    private static class GlobalProperty<ValueType> implements MutableProperty<ValueType> {
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
        public void setValue(ValueType value) {
            String strValue = converter.toString(value);
            if (strValue != null) {
                getPreferences().put(settingsName, strValue);
            }
            else {
                getPreferences().remove(settingsName);
            }
        }

        @Override
        public ValueType getValue() {
            return converter.toValue(getPreferences().get(settingsName, null));
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
