package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.tasks.BuiltInTasks;

public final class DefaultProjectProperties extends AbstractProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(DefaultProjectProperties.class.getName());

    // Currently not used but might be used later for determining the source
    // level from the models.
    private final NbGradleProject project;

    public DefaultProjectProperties(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public MutableProperty<String> getSourceLevel() {
        return new UnmodifiableProperty<String>("SourceLevel") {
            @Override
            public String getValue() {
                return getSourceLevelFromPlatform(getPlatform().getValue());
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                getPlatform().addChangeListener(listener);
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                getPlatform().removeChangeListener(listener);
            }
        };
    }

    @Override
    public MutableProperty<JavaPlatform> getPlatform() {
        return new UnmodifiableProperty<JavaPlatform>("Platform") {
            @Override
            public JavaPlatform getValue() {
                return JavaPlatform.getDefault();
            }
        };
    }

    @Override
    public MutableProperty<JavaPlatform> getScriptPlatform() {
        return new WrappedUnmodifiableProperty<JavaPlatform>("ScriptPlatform", GlobalGradleSettings.getGradleJdk());
    }

    @Override
    public MutableProperty<GradleLocation> getGradleLocation() {
        return new WrappedUnmodifiableProperty<GradleLocation>("GradleLocation", GlobalGradleSettings.getGradleHome());
    }

    @Override
    public MutableProperty<Charset> getSourceEncoding() {
        return new UnmodifiableProperty<Charset>("SourceEncoding") {
            @Override
            public Charset getValue() {
                return DEFAULT_SOURCE_ENCODING;
            }
        };
    }

    @Override
    public MutableProperty<List<PredefinedTask>> getCommonTasks() {
        return new UnmodifiableProperty<List<PredefinedTask>>("CommonTasks") {
            @Override
            public List<PredefinedTask> getValue() {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public MutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
        final PredefinedTask task = BuiltInTasks.getDefaultBuiltInTask(command);
        return new UnmodifiableProperty<PredefinedTask>("BuiltInTask-" + command) {
            @Override
            public PredefinedTask getValue() {
                return task;
            }
        };
    }

    private static final class WrappedUnmodifiableProperty<ValueType> extends UnmodifiableProperty<ValueType> {
        private final MutableProperty<ValueType> wrapped;

        public WrappedUnmodifiableProperty(String propertyName, MutableProperty<ValueType> wrapped) {
            super(propertyName);

            if (wrapped == null) throw new NullPointerException("wrapped");
            this.wrapped = wrapped;
        }

        @Override
        public ValueType getValue() {
            return wrapped.getValue();
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            wrapped.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            wrapped.removeChangeListener(listener);
        }
    }

    private static abstract class UnmodifiableProperty<ValueType> implements MutableProperty<ValueType> {
        private final String propertyName;

        public UnmodifiableProperty(String propertyName) {
            if (propertyName == null) throw new NullPointerException("propertyName");
            this.propertyName = propertyName;
        }

        @Override
        public final void setValueFromSource(PropertySource<? extends ValueType> source) {
            LOGGER.log(Level.WARNING, "Attempting to modify a default property: {0}", propertyName);
        }

        @Override
        public final void setValue(ValueType value) {
            LOGGER.log(Level.WARNING, "Attempting to modify a default property: {0}", propertyName);
        }

        @Override
        public final boolean isDefault() {
            return true;
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
        }
    }
}
