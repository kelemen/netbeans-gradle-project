package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.persistent.PropertiesPersister;
import org.openide.util.ChangeSupport;
import org.w3c.dom.Element;

public final class ProjectPropertiesProxy extends AbstractProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(ProjectPropertiesProxy.class.getName());

    private final NbGradleProject project;
    private final AtomicReference<ProjectProperties> propertiesRef;
    private final ChangeSupport changes;

    private final MutablePropertyProxy<String> sourceLevelProxy;
    private final MutablePropertyProxy<ProjectPlatform> platformProxy;
    private final MutablePropertyProxy<JavaPlatform> scriptPlatformProxy;
    private final MutablePropertyProxy<GradleLocation> gradleHomeProxy;
    private final MutablePropertyProxy<Charset> sourceEncodingProxy;
    private final MutablePropertyProxy<List<PredefinedTask>> commonTasksProxy;
    private final MutablePropertyProxy<LicenseHeaderInfo> licenseHeaderProxy;
    private final MutablePropertyProxy<Void> auxConfigListener;
    private final Map<String, MutablePropertyProxy<PredefinedTask>> builtInTasks;
    private final ConcurrentMap<DomElementKey, AuxConfigProperty> auxProperties;
    private final WaitableSignal loadedSignal;

    public ProjectPropertiesProxy(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.propertiesRef = new AtomicReference<ProjectProperties>(null);
        this.changes = new ChangeSupport(this);
        this.loadedSignal = new WaitableSignal();

        this.auxProperties = new ConcurrentHashMap<DomElementKey, AuxConfigProperty>();
        this.auxConfigListener = new MutablePropertyProxy<Void>(new ProjectMutablePropertyRef<Void>(this) {
            @Override
            public MutableProperty<Void> getProperty() {
                return this.getProperties().getAuxConfigListener();
            }
        });
        this.sourceLevelProxy = new MutablePropertyProxy<String>(new ProjectMutablePropertyRef<String>(this) {
            @Override
            public MutableProperty<String> getProperty() {
                return this.getProperties().getSourceLevel();
            }
        });
        this.platformProxy = new MutablePropertyProxy<ProjectPlatform>(new ProjectMutablePropertyRef<ProjectPlatform>(this) {
            @Override
            public MutableProperty<ProjectPlatform> getProperty() {
                return this.getProperties().getPlatform();
            }
        });
        this.scriptPlatformProxy = new MutablePropertyProxy<JavaPlatform>(new ProjectMutablePropertyRef<JavaPlatform>(this) {
            @Override
            public MutableProperty<JavaPlatform> getProperty() {
                return this.getProperties().getScriptPlatform();
            }
        });
        this.gradleHomeProxy = new MutablePropertyProxy<GradleLocation>(new ProjectMutablePropertyRef<GradleLocation>(this) {
            @Override
            public MutableProperty<GradleLocation> getProperty() {
                return this.getProperties().getGradleLocation();
            }
        });
        this.sourceEncodingProxy = new MutablePropertyProxy<Charset>(new ProjectMutablePropertyRef<Charset>(this) {
            @Override
            public MutableProperty<Charset> getProperty() {
                return this.getProperties().getSourceEncoding();
            }
        });
        this.licenseHeaderProxy = new MutablePropertyProxy<LicenseHeaderInfo>(new ProjectMutablePropertyRef<LicenseHeaderInfo>(this) {
            @Override
            public MutableProperty<LicenseHeaderInfo> getProperty() {
                return this.getProperties().getLicenseHeader();
            }
        });
        this.commonTasksProxy = new MutablePropertyProxy<List<PredefinedTask>>(new ProjectMutablePropertyRef<List<PredefinedTask>>(this) {
            @Override
            public MutableProperty<List<PredefinedTask>> getProperty() {
                return this.getProperties().getCommonTasks();
            }
        });

        Set<String> commands = AbstractProjectProperties.getCustomizableCommands();
        this.builtInTasks = new HashMap<String, MutablePropertyProxy<PredefinedTask>>(2 * commands.size());
        for (final String command: commands) {
            MutablePropertyProxy<PredefinedTask> proxy = new MutablePropertyProxy<PredefinedTask>(new ProjectMutablePropertyRef<PredefinedTask>(this) {
                @Override
                public MutableProperty<PredefinedTask> getProperty() {
                    MutableProperty<PredefinedTask> taskProperty = this.getProperties().tryGetBuiltInTask(command);
                    if (taskProperty == null) {
                        throw new IllegalStateException("Missing customizable command: " + command);
                    }
                    return taskProperty;
                }
            });
            this.builtInTasks.put(command, proxy);
        }
    }

    public boolean tryWaitForLoaded() {
        // Attempting to call this method from any of the threads below could
        // cause a dead-lock.
        if (NbGradleProject.PROJECT_PROCESSOR.isRequestProcessorThread()) {
            throw new IllegalStateException("This method cannot be access from the PROJECT_PROCESSOR.");
        }
        if (PropertiesPersister.PERSISTER_PROCESSOR.isRequestProcessorThread()) {
            throw new IllegalStateException("This method cannot be access from the PERSISTER_PROCESSOR.");
        }

        getProperties();
        return loadedSignal.tryWaitForSignal();
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
                        changes.fireChange();
                    }
                };

                project.addModelChangeListener(reloadTask);
                project.addProfileChangeListener(reloadTask);
            }

            properties = propertiesRef.get();
        }
        return properties;
    }

    private void addModelChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    private void removeModelChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    @Override
    public MutableProperty<LicenseHeaderInfo> getLicenseHeader() {
        return licenseHeaderProxy;
    }

    @Override
    public MutableProperty<String> getSourceLevel() {
        return sourceLevelProxy;
    }

    @Override
    public MutableProperty<ProjectPlatform> getPlatform() {
        return platformProxy;
    }

    @Override
    public MutableProperty<JavaPlatform> getScriptPlatform() {
        return scriptPlatformProxy;
    }

    @Override
    public MutableProperty<GradleLocation> getGradleLocation() {
        return gradleHomeProxy;
    }

    @Override
    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncodingProxy;
    }

    @Override
    public MutableProperty<List<PredefinedTask>> getCommonTasks() {
        return commonTasksProxy;
    }

    @Override
    public MutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
        if (command == null) throw new NullPointerException("command");
        return builtInTasks.get(command);
    }

    @Override
    public AuxConfigProperty getAuxConfig(final String elementName, final String namespace) {
        DomElementKey key = new DomElementKey(elementName, namespace);
        AuxConfigProperty property = auxProperties.get(key);
        if (property != null) {
            return property;
        }

        MutablePropertyProxy<Element> proxy = new MutablePropertyProxy<Element>(new ProjectMutablePropertyRef<Element>(this) {
            @Override
            public MutableProperty<Element> getProperty() {
                return this.getProperties().getAuxConfig(elementName, namespace).getProperty();
            }
        });
        auxProperties.putIfAbsent(key, new AuxConfigProperty(key, proxy));
        return auxProperties.get(key);
    }

    @Override
    public MutableProperty<Void> getAuxConfigListener() {
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
        return new ArrayList<AuxConfigProperty>(auxProperties.values());
    }

    private static interface MutablePropertyRef<ValueType> {
        public MutableProperty<ValueType> getProperty();

        public void addChangeListener(ChangeListener listener);
        public void removeChangeListener(ChangeListener listener);
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
        public final void addChangeListener(ChangeListener listener) {
            parent.addModelChangeListener(listener);
        }

        @Override
        public final void removeChangeListener(ChangeListener listener) {
            parent.removeModelChangeListener(listener);
        }
    }

    private static class MutablePropertyProxy<ValueType> implements MutableProperty<ValueType> {
        private final MutablePropertyRef<ValueType> propertyRef;

        private final Lock mainLock;
        private MutableProperty<?> forwardingTo;
        private final ChangeListener forwarder;
        private final List<ChangeListener> listeners;

        public MutablePropertyProxy(MutablePropertyRef<ValueType> propertyRef) {
            this.propertyRef = propertyRef;
            this.mainLock = new ReentrantLock();
            this.listeners = new LinkedList<ChangeListener>();
            this.forwardingTo = null;
            this.forwarder = new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    List<ChangeListener> listenersCopy = null;
                    mainLock.lock();
                    try {
                        if (!listeners.isEmpty()) {
                            listenersCopy = new ArrayList<ChangeListener>(listeners);
                        }
                    } finally {
                        mainLock.unlock();
                    }

                    if (listenersCopy != null) {
                        for (ChangeListener listener: listenersCopy) {
                            try {
                                listener.stateChanged(e);
                            } catch (Throwable ex) {
                                LOGGER.log(Level.SEVERE, "Unexpected exception in a listener.", ex);
                            }
                        }
                    }
                }
            };
        }

        @Override
        public void setValueFromSource(PropertySource<? extends ValueType> source) {
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

        @Override
        public void addChangeListener(ChangeListener listener) {
            mainLock.lock();
            try {
                if (listeners.isEmpty()) {
                    forwardingTo = propertyRef.getProperty();
                    forwardingTo.addChangeListener(forwarder);
                    propertyRef.addChangeListener(forwarder);
                }
                listeners.add(listener);
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            mainLock.lock();
            try {
                if (listeners.isEmpty()) {
                    return;
                }

                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    if (forwardingTo != null) {
                        propertyRef.removeChangeListener(forwarder);
                        forwardingTo.removeChangeListener(listener);
                        forwardingTo = null;
                    }
                    else {
                        String logMessage = "Cannot remove forwarding listener.";
                        LOGGER.log(Level.SEVERE, logMessage, new IllegalStateException(logMessage));
                    }
                }
            } finally {
                mainLock.unlock();
            }
        }
    }
}
