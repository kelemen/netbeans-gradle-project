package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.tasks.BuiltInTasks;

public final class MemProjectProperties extends AbstractProjectProperties {
    private final MutableProperty<String> sourceLevel;
    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<JavaPlatform> scriptPlatform;
    private final MutableProperty<GradleLocation> gradleHome;
    private final MutableProperty<Charset> sourceEncoding;
    private final MutableProperty<LicenseHeaderInfo> licenseHeader;
    private final MutableProperty<Void> auxConfigListener;
    private final MutableProperty<List<PredefinedTask>> commonTasks;
    private final Map<String, MutableProperty<PredefinedTask>> builtInTasks;
    private final ConcurrentMap<DomElementKey, AuxConfigProperty> auxProperties;

    public MemProjectProperties() {
        JavaPlatform defaultPlatform = JavaPlatform.getDefault();
        this.sourceLevel = new DefaultMutableProperty<String>(getSourceLevelFromPlatform(defaultPlatform), true, false);
        this.platform = new DefaultMutableProperty<JavaPlatform>(defaultPlatform, true, false);
        this.scriptPlatform = new DefaultMutableProperty<JavaPlatform>(defaultPlatform, true, false);
        this.gradleHome = new DefaultMutableProperty<GradleLocation>(GradleLocationDefault.INSTANCE, true, false);
        this.licenseHeader = new DefaultMutableProperty<LicenseHeaderInfo>(null, true, true);
        this.auxConfigListener = new DefaultMutableProperty<Void>(null, true, true);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(DEFAULT_SOURCE_ENCODING, true, false);
        this.commonTasks = new MutableListProperty<PredefinedTask>(Collections.<PredefinedTask>emptyList(), true);
        this.auxProperties = new ConcurrentHashMap<DomElementKey, AuxConfigProperty>();

        Set<String> commands = AbstractProjectProperties.getCustomizableCommands();
        this.builtInTasks = new HashMap<String, MutableProperty<PredefinedTask>>(2 * commands.size());
        for (String command: commands) {
            PredefinedTask task = BuiltInTasks.getDefaultBuiltInTask(command);
            this.builtInTasks.put(command,
                    new DefaultMutableProperty<PredefinedTask>(task, true, false));
        }
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
    public MutableProperty<JavaPlatform> getPlatform() {
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

        return builtInTasks.get(command);
    }

    @Override
    public MutableProperty<Void> getAuxConfigListener() {
        return auxConfigListener;
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
