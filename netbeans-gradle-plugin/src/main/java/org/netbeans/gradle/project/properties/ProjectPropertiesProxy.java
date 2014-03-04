package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.w3c.dom.Element;

public final class ProjectPropertiesProxy extends AbstractProjectProperties {
    private final NbGradleProject project;
    private final AtomicReference<ProjectProperties> propertiesRef;
    private final ListenerManager<Runnable> changes;

    private final MutablePropertyProxy<String> sourceLevelProxy;
    private final MutablePropertyProxy<ProjectPlatform> platformProxy;
    private final MutablePropertyProxy<JavaPlatform> scriptPlatformProxy;
    private final MutablePropertyProxy<GradleLocation> gradleHomeProxy;
    private final MutablePropertyProxy<Charset> sourceEncodingProxy;
    private final MutablePropertyProxy<List<PredefinedTask>> commonTasksProxy;
    private final MutablePropertyProxy<LicenseHeaderInfo> licenseHeaderProxy;
    private final MutablePropertyProxy<Void> auxConfigListener;
    private final ConcurrentMap<String, MutablePropertyProxy<PredefinedTask>> builtInTasks;
    private final ConcurrentMap<DomElementKey, AuxConfigProperty> auxProperties;
    private final WaitableSignal loadedSignal;

    public ProjectPropertiesProxy(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;
        this.propertiesRef = new AtomicReference<>(null);
        this.changes = new CopyOnTriggerListenerManager<>();
        this.loadedSignal = new WaitableSignal();

        this.auxProperties = new ConcurrentHashMap<>();
        this.auxConfigListener = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<Void>(this) {
            @Override
            public OldMutableProperty<Void> getProperty() {
                return super.getProperties().getAuxConfigListener();
            }
        });
        this.sourceLevelProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<String>(this) {
            @Override
            public OldMutableProperty<String> getProperty() {
                return super.getProperties().getSourceLevel();
            }
        });
        this.platformProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<ProjectPlatform>(this) {
            @Override
            public OldMutableProperty<ProjectPlatform> getProperty() {
                return super.getProperties().getPlatform();
            }
        });
        this.scriptPlatformProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<JavaPlatform>(this) {
            @Override
            public OldMutableProperty<JavaPlatform> getProperty() {
                return super.getProperties().getScriptPlatform();
            }
        });
        this.gradleHomeProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<GradleLocation>(this) {
            @Override
            public OldMutableProperty<GradleLocation> getProperty() {
                return super.getProperties().getGradleLocation();
            }
        });
        this.sourceEncodingProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<Charset>(this) {
            @Override
            public OldMutableProperty<Charset> getProperty() {
                return super.getProperties().getSourceEncoding();
            }
        });
        this.licenseHeaderProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<LicenseHeaderInfo>(this) {
            @Override
            public OldMutableProperty<LicenseHeaderInfo> getProperty() {
                return super.getProperties().getLicenseHeader();
            }
        });
        this.commonTasksProxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<List<PredefinedTask>>(this) {
            @Override
            public OldMutableProperty<List<PredefinedTask>> getProperty() {
                return super.getProperties().getCommonTasks();
            }
        });

        this.builtInTasks = new ConcurrentHashMap<>();
    }

    public boolean isLoaded() {
        return loadedSignal.isSignaled();
    }

    public void waitForLoaded(CancellationToken cancelToken) {
        // Attempting to call this method from any of the threads below could
        // cause a dead-lock.
        if (NbGradleProject.PROJECT_PROCESSOR.isExecutingInThis()) {
            throw new IllegalStateException("This method cannot be access from the PROJECT_PROCESSOR.");
        }
        if (PropertiesPersister.PERSISTER_PROCESSOR.isExecutingInThis()) {
            throw new IllegalStateException("This method cannot be access from the PERSISTER_PROCESSOR.");
        }

        getProperties();
        loadedSignal.waitSignal(cancelToken);
    }

    private ProjectProperties getProperties() {
        ProjectProperties properties = propertiesRef.get();
        if (properties == null) {
            properties = ProjectPropertiesManager.getProperties(project, loadedSignal);
            if (propertiesRef.compareAndSet(null, properties)) {
                ChangeListener reloadTask = new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        propertiesRef.set(ProjectPropertiesManager.getProperties(project, loadedSignal));
                        EventListeners.dispatchRunnable(changes);
                    }
                };

                project.addModelChangeListener(reloadTask);
                project.addProfileChangeListener(reloadTask);
            }

            properties = propertiesRef.get();
        }
        return properties;
    }

    private ListenerRef addModelChangeListener(Runnable listener) {
        return changes.registerListener(listener);
    }

    @Override
    public OldMutableProperty<LicenseHeaderInfo> getLicenseHeader() {
        return licenseHeaderProxy;
    }

    @Override
    public OldMutableProperty<String> getSourceLevel() {
        return sourceLevelProxy;
    }

    @Override
    public OldMutableProperty<ProjectPlatform> getPlatform() {
        return platformProxy;
    }

    @Override
    public OldMutableProperty<JavaPlatform> getScriptPlatform() {
        return scriptPlatformProxy;
    }

    @Override
    public OldMutableProperty<GradleLocation> getGradleLocation() {
        return gradleHomeProxy;
    }

    @Override
    public OldMutableProperty<Charset> getSourceEncoding() {
        return sourceEncodingProxy;
    }

    @Override
    public OldMutableProperty<List<PredefinedTask>> getCommonTasks() {
        return commonTasksProxy;
    }

    @Override
    public Set<String> getKnownBuiltInCommands() {
        return getProperties().getKnownBuiltInCommands();
    }

    @Override
    public OldMutableProperty<PredefinedTask> tryGetBuiltInTask(final String command) {
        ExceptionHelper.checkNotNullArgument(command, "command");

        OldMutableProperty<PredefinedTask> result = builtInTasks.get(command);
        if (result == null) {
            MutablePropertyProxy<PredefinedTask> proxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<PredefinedTask>(this) {
                @Override
                public OldMutableProperty<PredefinedTask> getProperty() {
                    OldMutableProperty<PredefinedTask> taskProperty = super.getProperties().tryGetBuiltInTask(command);
                    if (taskProperty == null) {
                        throw new IllegalStateException("Missing customizable command: " + command);
                    }
                    return taskProperty;
                }
            });
            builtInTasks.putIfAbsent(command, proxy);
            result = builtInTasks.get(command);
        }
        return result;
    }

    @Override
    public AuxConfigProperty getAuxConfig(final String elementName, final String namespace) {
        DomElementKey key = new DomElementKey(elementName, namespace);
        AuxConfigProperty property = auxProperties.get(key);
        if (property != null) {
            return property;
        }

        MutablePropertyProxy<Element> proxy = new MutablePropertyProxy<>(new ProjectMutablePropertyRef<Element>(this) {
            @Override
            public OldMutableProperty<Element> getProperty() {
                return super.getProperties().getAuxConfig(elementName, namespace).getProperty();
            }
        });
        auxProperties.putIfAbsent(key, new AuxConfigProperty(key, proxy));
        return auxProperties.get(key);
    }

    @Override
    public OldMutableProperty<Void> getAuxConfigListener() {
        return auxConfigListener;
    }

    @Override
    public void setAllAuxConfigs(Collection<AuxConfig> configs) {
        for (AuxConfigProperty property: auxProperties.values()) {
            property.getProperty().setValue(null);
        }
        for (AuxConfig config: configs) {
            DomElementKey key = config.getKey();
            getAuxConfig(key.getName(), key.getNamespace()).getProperty().setValue(config.getValue());
        }
    }

    @Override
    public Collection<AuxConfigProperty> getAllAuxConfigs() {
        return new ArrayList<>(auxProperties.values());
    }

    private static interface MutablePropertyRef<ValueType> {
        public OldMutableProperty<ValueType> getProperty();

        public ListenerRef addChangeListener(Runnable listener);
    }

    private static abstract class ProjectMutablePropertyRef<ValueType>
    implements
            MutablePropertyRef<ValueType> {

        private final ProjectPropertiesProxy parent;

        public ProjectMutablePropertyRef(ProjectPropertiesProxy parent) {
            this.parent = parent;
        }

        protected final ProjectProperties getProperties() {
            return parent.getProperties();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return parent.addModelChangeListener(listener);
        }
    }

    private static class MutablePropertyProxy<ValueType> implements OldMutableProperty<ValueType> {
        private final MutablePropertyRef<ValueType> propertyRef;

        public MutablePropertyProxy(MutablePropertyRef<ValueType> propertyRef) {
            this.propertyRef = propertyRef;
        }

        @Override
        public void setValueFromSource(OldPropertySource<? extends ValueType> source) {
            propertyRef.getProperty().setValueFromSource(source);
        }

        @Override
        public void setValue(ValueType value) {
            propertyRef.getProperty().setValue(value);
        }

        @Override
        public ValueType getValue() {
            return propertyRef.getProperty().getValue();
        }

        @Override
        public boolean isDefault() {
            return propertyRef.getProperty().isDefault();
        }

        private void registerWithSubListener(Runnable listener, AtomicReference<ListenerRef> subListenerRef) {
            ListenerRef newRef = propertyRef.getProperty().addChangeListener(listener);
            ListenerRef prevRef = subListenerRef.getAndSet(newRef);
            if (prevRef != null) {
                prevRef.unregister();
            }
            else {
                subListenerRef.compareAndSet(newRef, null);
                newRef.unregister();
            }
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            // null means that the client unregistered
            final AtomicReference<ListenerRef> subListenerRef
                    = new AtomicReference<ListenerRef>(UnregisteredListenerRef.INSTANCE);

            final ListenerRef listenerRef = propertyRef.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    registerWithSubListener(listener, subListenerRef);
                    listener.run();
                }
            });

            registerWithSubListener(listener, subListenerRef);

            return NbListenerRefs.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    listenerRef.unregister();
                    ListenerRef subRef = subListenerRef.getAndSet(null);
                    if (subRef != null) {
                        subRef.unregister();
                    }
                }
            });
        }
    }
}
