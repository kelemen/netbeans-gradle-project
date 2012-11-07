package org.netbeans.gradle.project.properties;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.openide.util.ChangeSupport;

public final class ProjectPropertiesProxy extends AbstractProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(ProjectPropertiesProxy.class.getName());

    private final NbGradleProject project;
    private final AtomicReference<ProjectProperties> propertiesRef;
    private final ChangeSupport changes;

    private final MutablePropertyProxy<String> sourceLevelProxy;
    private final MutablePropertyProxy<JavaPlatform> platformProxy;
    private final MutablePropertyProxy<Charset> sourceEncodingProxy;
    private final MutablePropertyProxy<List<PredefinedTask>> commonTasksProxy;
    private final Map<String, MutablePropertyProxy<PredefinedTask>> builtInTasks;
    private final WaitableSignal loadedSignal;

    public ProjectPropertiesProxy(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.propertiesRef = new AtomicReference<ProjectProperties>(null);
        this.changes = new ChangeSupport(this);
        this.loadedSignal = new WaitableSignal();

        this.sourceLevelProxy = new MutablePropertyProxy<String>(new ProjectMutablePropertyRef<String>(this) {
            @Override
            public MutableProperty<String> getProperty() {
                return this.getProperties().getSourceLevel();
            }
        });
        this.platformProxy = new MutablePropertyProxy<JavaPlatform>(new ProjectMutablePropertyRef<JavaPlatform>(this) {
            @Override
            public MutableProperty<JavaPlatform> getProperty() {
                return this.getProperties().getPlatform();
            }
        });
        this.sourceEncodingProxy = new MutablePropertyProxy<Charset>(new ProjectMutablePropertyRef<Charset>(this) {
            @Override
            public MutableProperty<Charset> getProperty() {
                return this.getProperties().getSourceEncoding();
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
        getProperties();
        return loadedSignal.tryWaitForSignal();
    }

    private ProjectProperties getProperties() {
        ProjectProperties properties = propertiesRef.get();
        if (properties == null) {
            File[] propertiesFiles = SettingsFiles.getFilesForProject(project);
            properties = ProjectPropertiesManager.getProperties(propertiesFiles, loadedSignal);
            if (propertiesRef.compareAndSet(null, properties)) {
                ChangeListener reloadTask = new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        File[] propertiesFiles = SettingsFiles.getFilesForProject(project);
                        propertiesRef.set(ProjectPropertiesManager.getProperties(propertiesFiles, loadedSignal));
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
    public MutableProperty<String> getSourceLevel() {
        return sourceLevelProxy;
    }

    @Override
    public MutableProperty<JavaPlatform> getPlatform() {
        return platformProxy;
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
