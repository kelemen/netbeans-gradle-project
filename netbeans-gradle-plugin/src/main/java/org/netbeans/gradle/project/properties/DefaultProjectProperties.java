package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;
import org.w3c.dom.Element;

public final class DefaultProjectProperties extends AbstractProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(DefaultProjectProperties.class.getName());

    private final NbGradleProject project;
    private final JavaExtension javaExt;

    public DefaultProjectProperties(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;
        this.javaExt = project.getLookup().lookup(JavaExtension.class);
    }

    @Override
    public OldMutableProperty<LicenseHeaderInfo> getLicenseHeader() {
        return new UnmodifiableProperty<LicenseHeaderInfo>("LicenseHeader") {
            @Override
            public LicenseHeaderInfo getValue() {
                return null;
            }
        };
    }

    private J2SEPlatformFromScriptQuery tryGetPlatformScriptQuery() {
        return project.getCombinedExtensionLookup().lookup(J2SEPlatformFromScriptQuery.class);
    }

    private String tryGetScriptSourceLevel() {
        J2SEPlatformFromScriptQuery query = tryGetPlatformScriptQuery();
        return query != null ? query.getSourceLevel() : null;
    }

    private ProjectPlatform tryGetScriptPlatform() {
        J2SEPlatformFromScriptQuery query = tryGetPlatformScriptQuery();
        return query != null ? query.getPlatform(): null;
    }

    private boolean isReliableJavaVersion() {
        return javaExt.getCurrentModel().getModelSource().isReliableJavaVersion()
                || GlobalGradleSettings.getMayRelyOnJavaOfScript().getValue();
    }

    @Override
    public OldMutableProperty<String> getSourceLevel() {
        return new UnmodifiableProperty<String>("SourceLevel") {
            @Override
            public String getValue() {
                if (isReliableJavaVersion()) {
                    String sourceLevel = tryGetScriptSourceLevel();
                    if (sourceLevel != null) {
                        return sourceLevel;
                    }
                }
                return getSourceLevelFromPlatform(getPlatform().getValue());
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                ListenerRef ref1 = GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
                ListenerRef ref2 = project.addModelChangeListener(listener);
                ListenerRef ref3 = getPlatform().addChangeListener(listener);

                return ListenerRegistries.combineListenerRefs(ref1, ref2, ref3);
            }
        };
    }

    @Override
    public OldMutableProperty<ProjectPlatform> getPlatform() {
        return new UnmodifiableProperty<ProjectPlatform>("Platform") {
            // This is here only to register and remove listeners because
            // it can detect changes in the list of platforms defined in
            // NetBeans. We will never request the value of this property
            // source, so the actual parameters do not matter.
            private final OldPropertySource<?> platformListHelper
                    = DefaultPropertySources.findPlatformSource("j2se", "1.3", true);

            @Override
            public ProjectPlatform getValue() {
                if (isReliableJavaVersion()) {
                    ProjectPlatform platform = tryGetScriptPlatform();
                    if (platform != null) {
                        return platform;
                    }
                }
                return AbstractProjectPlatformSource.getDefaultPlatform();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                ListenerRef ref1 = GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
                ListenerRef ref2 = project.addModelChangeListener(listener);
                ListenerRef ref3 = platformListHelper.addChangeListener(listener);

                return ListenerRegistries.combineListenerRefs(ref1, ref2, ref3);
            }
        };
    }

    @Override
    public OldMutableProperty<JavaPlatform> getScriptPlatform() {
        return new WrappedUnmodifiableProperty<>("ScriptPlatform", GlobalGradleSettings.getGradleJdk());
    }

    @Override
    public OldMutableProperty<GradleLocation> getGradleLocation() {
        return new WrappedUnmodifiableProperty<>("GradleLocation", GlobalGradleSettings.getGradleHome());
    }

    @Override
    public OldMutableProperty<Charset> getSourceEncoding() {
        return new UnmodifiableProperty<Charset>("SourceEncoding") {
            @Override
            public Charset getValue() {
                return DEFAULT_SOURCE_ENCODING;
            }
        };
    }

    @Override
    public OldMutableProperty<List<PredefinedTask>> getCommonTasks() {
        return new UnmodifiableProperty<List<PredefinedTask>>("CommonTasks") {
            @Override
            public List<PredefinedTask> getValue() {
                return Collections.emptyList();
            }
        };
    }

    private static PredefinedTask templateToPredefined(
            String displayName, GradleCommandTemplate command) {
        List<PredefinedTask.Name> taskNames = new LinkedList<>();
        for (String taskName: command.getTasks()) {
            taskNames.add(new PredefinedTask.Name(taskName, false));
        }

        return new PredefinedTask(displayName,
                taskNames,
                command.getArguments(),
                command.getJvmArguments(),
                !command.isBlocking());
    }

    @Override
    public OldMutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
        ProfileDef profile = project.getCurrentProfile().getProfileDef();
        GradleCommandTemplate commandTemplate
                = project.getMergedCommandQuery().tryGetDefaultGradleCommand(profile, command);

        final PredefinedTask task = commandTemplate != null
                ? templateToPredefined(command, commandTemplate)
                : null;
        return new UnmodifiableProperty<PredefinedTask>("BuiltInTask-" + command) {
            @Override
            public PredefinedTask getValue() {
                return task;
            }
        };
    }

    @Override
    public Set<String> getKnownBuiltInCommands() {
        return project.getMergedCommandQuery().getSupportedCommands();
    }

    @Override
    public OldMutableProperty<Void> getAuxConfigListener() {
        return new UnmodifiableProperty<Void>("AuxConfigListener") {
            @Override
            public Void getValue() {
                return null;
            }
        };
    }

    @Override
    public AuxConfigProperty getAuxConfig(String elementName, String namespace) {
        UnmodifiableProperty<Element> property = new UnmodifiableProperty<Element>("AuxConfig-" + elementName) {
            @Override
            public Element getValue() {
                return null;
            }
        };
        return new AuxConfigProperty(
                new DomElementKey(elementName, namespace),
                property);
    }

    @Override
    public void setAllAuxConfigs(Collection<AuxConfig> configs) {
        LOGGER.log(Level.WARNING, "Attempting to modify a default property: AuxConfigs");
    }

    @Override
    public Collection<AuxConfigProperty> getAllAuxConfigs() {
        return Collections.emptyList();
    }

    private static final class WrappedUnmodifiableProperty<ValueType> extends UnmodifiableProperty<ValueType> {
        private final MutableProperty<ValueType> wrapped;

        public WrappedUnmodifiableProperty(String propertyName, MutableProperty<ValueType> wrapped) {
            super(propertyName);

            ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
            this.wrapped = wrapped;
        }

        @Override
        public ValueType getValue() {
            return wrapped.getValue();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return wrapped.addChangeListener(listener);
        }
    }

    private static abstract class UnmodifiableProperty<ValueType> implements OldMutableProperty<ValueType> {
        private final String propertyName;

        public UnmodifiableProperty(String propertyName) {
            ExceptionHelper.checkNotNullArgument(propertyName, "propertyName");
            this.propertyName = propertyName;
        }

        @Override
        public final void setValueFromSource(OldPropertySource<? extends ValueType> source) {
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
        public ListenerRef addChangeListener(Runnable listener) {
            return UnregisteredListenerRef.INSTANCE;
        }
    }
}
