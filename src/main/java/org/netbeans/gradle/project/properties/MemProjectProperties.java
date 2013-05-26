package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;

public final class MemProjectProperties extends AbstractProjectProperties {
    private final MutableProperty<String> sourceLevel;
    private final MutableProperty<ProjectPlatform> platform;
    private final MutableProperty<JavaPlatform> scriptPlatform;
    private final MutableProperty<GradleLocation> gradleHome;
    private final MutableProperty<Charset> sourceEncoding;
    private final MutableProperty<LicenseHeaderInfo> licenseHeader;
    private final MutableProperty<Void> auxConfigListener;
    private final MutableProperty<List<PredefinedTask>> commonTasks;
    private final ConcurrentMap<String, MutableProperty<PredefinedTask>> builtInTasks;
    private final ConcurrentMap<DomElementKey, AuxConfigProperty> auxProperties;

    private final MutableProperty<Object> builtInChangeSignal;

    public MemProjectProperties() {
        ProjectPlatform defaultPlatform = AbstractProjectPlatformSource.getDefaultPlatform();
        JavaPlatform defaultJavaPlatform = JavaPlatform.getDefault();

        this.builtInChangeSignal = new DefaultMutableProperty<Object>(new Object(), true, false);
        this.sourceLevel = new DefaultMutableProperty<String>(getSourceLevelFromPlatform(defaultPlatform), true, false);
        this.platform = new DefaultMutableProperty<ProjectPlatform>(defaultPlatform, true, false);
        this.scriptPlatform = new DefaultMutableProperty<JavaPlatform>(defaultJavaPlatform, true, false);
        this.gradleHome = new DefaultMutableProperty<GradleLocation>(GradleLocationDefault.INSTANCE, true, false);
        this.licenseHeader = new DefaultMutableProperty<LicenseHeaderInfo>(null, true, true);
        this.auxConfigListener = new DefaultMutableProperty<Void>(null, true, true);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(DEFAULT_SOURCE_ENCODING, true, false);
        this.commonTasks = new MutableListProperty<PredefinedTask>(Collections.<PredefinedTask>emptyList(), true);
        this.auxProperties = new ConcurrentHashMap<DomElementKey, AuxConfigProperty>();
        this.builtInTasks = new ConcurrentHashMap<String, MutableProperty<PredefinedTask>>(32);
    }

    @Override
    public MutableProperty<LicenseHeaderInfo> getLicenseHeader() {
        return licenseHeader;
    }

    @Override
    public MutableProperty<String> getSourceLevel() {
        return sourceLevel;
    }

    @Override
    public MutableProperty<ProjectPlatform> getPlatform() {
        return platform;
    }

    @Override
    public MutableProperty<JavaPlatform> getScriptPlatform() {
        return scriptPlatform;
    }

    @Override
    public MutableProperty<GradleLocation> getGradleLocation() {
        return gradleHome;
    }

    @Override
    public MutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    @Override
    public MutableProperty<List<PredefinedTask>> getCommonTasks() {
        return commonTasks;
    }

    @Override
    public MutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
        if (command == null) throw new NullPointerException("command");

        MutableProperty<PredefinedTask> result = builtInTasks.get(command);
        if (result == null) {
            result = new DefaultMutableProperty<PredefinedTask>(null, true, true);
            result.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    builtInChangeSignal.setValue(new Object());
                }
            });

            builtInTasks.putIfAbsent(command, result);
            result = builtInTasks.get(command);
        }
        return result;
    }

    @Override
    public Set<String> getKnownBuiltInCommands() {
        return Collections.unmodifiableSet(builtInTasks.keySet());
    }

    @Override
    public MutableProperty<Void> getAuxConfigListener() {
        return auxConfigListener;
    }

    @Override
    public Collection<MutableProperty<?>> getAllProperties() {
        Collection<MutableProperty<?>> superProperties = super.getAllProperties();
        Collection<MutableProperty<?>> result = new ArrayList<MutableProperty<?>>(superProperties.size() + 1);
        result.addAll(superProperties);
        result.add(builtInChangeSignal);
        return result;
    }

    @Override
    public AuxConfigProperty getAuxConfig(String elementName, String namespace) {
        DomElementKey key = new DomElementKey(elementName, namespace);
        AuxConfigProperty property = auxProperties.get(key);
        if (property != null) {
            return property;
        }

        AuxConfigProperty newProperty = new AuxConfigProperty(key, new DomElementProperty());
        newProperty.getProperty().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getAuxConfigListener().setValue(null);
            }
        });
        auxProperties.putIfAbsent(key, newProperty);
        return auxProperties.get(key);
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
}
