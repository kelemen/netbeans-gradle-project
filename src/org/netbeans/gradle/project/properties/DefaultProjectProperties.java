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
                if (GlobalGradleSettings.getMayRelyOnJavaOfScript().getValue()) {
                    return project.getAvailableModel().getMainModule().getProperties().getSourceLevel();
                }
                else {
                    return getSourceLevelFromPlatform(getPlatform().getValue());
                }
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                getPlatform().addChangeListener(listener);
                project.addModelChangeListener(listener);
                GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                GlobalGradleSettings.getMayRelyOnJavaOfScript().removeChangeListener(listener);
                project.removeModelChangeListener(listener);
                getPlatform().removeChangeListener(listener);
            }
        };
    }

    @Override
    public MutableProperty<JavaPlatform> getPlatform() {
        return new UnmodifiableProperty<JavaPlatform>("Platform") {
            // This is here only te register and remove listeners because
            // it can detect changes in the list of platforms defined in
            // NetBeans. We will never request the value of this property
            // source, so the actual parameters do not matter.
            private final PropertySource<?> platformListHelper
                    = DefaultPropertySources.findPlatformSource("j2se", "1.3", true);

            @Override
            public JavaPlatform getValue() {
                if (GlobalGradleSettings.getMayRelyOnJavaOfScript().getValue()) {
                    String targetLevel = project.getAvailableModel().getMainModule().getProperties().getTargetLevel();
                    return DefaultPropertySources.findPlatformSource("j2se", targetLevel, true).getValue();
                }
                else {
                    return JavaPlatform.getDefault();
                }
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
                project.addModelChangeListener(listener);
                platformListHelper.addChangeListener(listener);

            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                platformListHelper.removeChangeListener(listener);
                project.removeModelChangeListener(listener);
                GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
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
