package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;

public final class MemProjectProperties extends AbstractProjectProperties {
    private final OldMutableProperty<String> sourceLevel;
    private final OldMutableProperty<ProjectPlatform> platform;
    private final OldMutableProperty<JavaPlatform> scriptPlatform;
    private final OldMutableProperty<GradleLocation> gradleHome;
    private final OldMutableProperty<Charset> sourceEncoding;
    private final OldMutableProperty<LicenseHeaderInfo> licenseHeader;
    private final OldMutableProperty<Void> auxConfigListener;
    private final OldMutableProperty<List<PredefinedTask>> commonTasks;
    private final ConcurrentMap<String, OldMutableProperty<PredefinedTask>> builtInTasks;
    private final ConcurrentMap<DomElementKey, AuxConfigProperty> auxProperties;

    private final OldMutableProperty<Object> builtInChangeSignal;

    public MemProjectProperties() {
        ProjectPlatform defaultPlatform = AbstractProjectPlatformSource.getDefaultPlatform();
        JavaPlatform defaultJavaPlatform = JavaPlatform.getDefault();

        this.builtInChangeSignal = new DefaultMutableProperty<>(new Object(), true, false);
        this.sourceLevel = new DefaultMutableProperty<>(getSourceLevelFromPlatform(defaultPlatform), true, false);
        this.platform = new DefaultMutableProperty<>(defaultPlatform, true, false);
        this.scriptPlatform = new DefaultMutableProperty<>(defaultJavaPlatform, true, false);
        this.gradleHome = new DefaultMutableProperty<GradleLocation>(GradleLocationDefault.INSTANCE, true, false);
        this.licenseHeader = new DefaultMutableProperty<>(null, true, true);
        this.auxConfigListener = new DefaultMutableProperty<>(null, true, true);
        this.sourceEncoding = new DefaultMutableProperty<>(DEFAULT_SOURCE_ENCODING, true, false);
        this.commonTasks = new MutableListProperty<>(Collections.<PredefinedTask>emptyList(), true);
        this.auxProperties = new ConcurrentHashMap<>();
        this.builtInTasks = new ConcurrentHashMap<>(32);
    }

    @Override
    public OldMutableProperty<LicenseHeaderInfo> getLicenseHeader() {
        return licenseHeader;
    }

    @Override
    public OldMutableProperty<String> getSourceLevel() {
        return sourceLevel;
    }

    @Override
    public OldMutableProperty<ProjectPlatform> getPlatform() {
        return platform;
    }

    @Override
    public OldMutableProperty<JavaPlatform> getScriptPlatform() {
        return scriptPlatform;
    }

    @Override
    public OldMutableProperty<GradleLocation> getGradleLocation() {
        return gradleHome;
    }

    @Override
    public OldMutableProperty<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    @Override
    public OldMutableProperty<List<PredefinedTask>> getCommonTasks() {
        return commonTasks;
    }

    @Override
    public OldMutableProperty<PredefinedTask> tryGetBuiltInTask(String command) {
        ExceptionHelper.checkNotNullArgument(command, "command");

        OldMutableProperty<PredefinedTask> result = builtInTasks.get(command);
        if (result == null) {
            result = new DefaultMutableProperty<>(null, true, true);
            result.addChangeListener(new Runnable() {
                @Override
                public void run() {
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
    public OldMutableProperty<Void> getAuxConfigListener() {
        return auxConfigListener;
    }

    @Override
    public Collection<OldMutableProperty<?>> getAllProperties() {
        Collection<OldMutableProperty<?>> superProperties = super.getAllProperties();
        Collection<OldMutableProperty<?>> result = new ArrayList<>(superProperties.size() + 1);
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
        newProperty.getProperty().addChangeListener(new Runnable() {
            @Override
            public void run() {
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
        return new ArrayList<>(auxProperties.values());
    }
}
