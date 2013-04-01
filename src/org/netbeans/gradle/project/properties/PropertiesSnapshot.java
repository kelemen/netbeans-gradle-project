package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.tasks.BuiltInTasks;
import org.w3c.dom.Element;

public final class PropertiesSnapshot {
    private static final Logger LOGGER = Logger.getLogger(PropertiesSnapshot.class.getName());

    public static final class Builder {
        private PropertySource<String> sourceLevel;
        private PropertySource<JavaPlatform> platform;
        private PropertySource<JavaPlatform> scriptPlatform;
        private PropertySource<GradleLocation> gradleHome;
        private PropertySource<Charset> sourceEncoding;
        private PropertySource<LicenseHeaderInfo> licenseHeader;
        private PropertySource<List<PredefinedTask>> commonTasks;
        private final Map<String, PropertySource<PredefinedTask>> builtInTasks;
        private final List<AuxConfigSource> auxProperties;

        public Builder() {
            this.platform = null;
            this.sourceEncoding = null;
            this.sourceLevel = null;
            this.scriptPlatform = null;
            this.gradleHome = null;
            this.commonTasks = null;
            this.licenseHeader = null;
            this.builtInTasks = new HashMap<String, PropertySource<PredefinedTask>>();
            this.auxProperties = new LinkedList<AuxConfigSource>();
        }

        public void addAuxConfig(AuxConfig config, boolean defaultValue) {
            if (config == null) throw new NullPointerException("config");
            auxProperties.add(new AuxConfigSource(
                    config.getKey(),
                    new DomElementSource(config.getValue(), defaultValue)));
        }

        public void addAuxProperty(AuxConfigSource source) {
            if (source == null) throw new NullPointerException("source");
            auxProperties.add(source);
        }

        public void addAuxProperty(AuxConfigProperty auxProperty) {
            if (auxProperty == null) throw new NullPointerException("auxProperty");

            MutableProperty<Element> property = auxProperty.getProperty();
            auxProperties.add(new AuxConfigSource(
                    auxProperty.getKey(),
                    new DomElementSource(property.getValue(), property.isDefault())));
        }

        public Collection<AuxConfigSource> getAuxProperties() {
            return new ArrayList<AuxConfigSource>(auxProperties);
        }

        public void setBuiltInTask(String command, PropertySource<PredefinedTask> task) {
            if (command == null) throw new NullPointerException("command");
            if (task == null) throw new NullPointerException("task");

            builtInTasks.put(command, task);
        }

        public PropertySource<PredefinedTask> getBuiltInTask(String command) {
            if (command == null) throw new NullPointerException("command");

            PropertySource<PredefinedTask> result = builtInTasks.get(command);
            return result != null
                    ? result
                    : asConstNullForNull(BuiltInTasks.getDefaultBuiltInTask(command), true);
        }

        public PropertySource<LicenseHeaderInfo> getLicenseHeader() {
            return licenseHeader != null
                    ? licenseHeader
                    : asConst((LicenseHeaderInfo)null, true);
        }

        public void setLicenseHeader(PropertySource<LicenseHeaderInfo> licenseFile) {
            if (licenseFile == null) throw new NullPointerException("licenseFile");
            this.licenseHeader = licenseFile;
        }

        public PropertySource<String> getSourceLevel() {
            return sourceLevel != null
                    ? sourceLevel
                    : DefaultPropertySources.parseSourceLevelSource(getPlatform(), true);
        }

        public void setSourceLevel(PropertySource<String> sourceLevel) {
            if (sourceLevel == null) throw new NullPointerException("sourceLevel");
            this.sourceLevel = sourceLevel;
        }

        public PropertySource<JavaPlatform> getPlatform() {
            return platform != null
                    ? platform
                    : asConst(JavaPlatform.getDefault(), true);
        }

        public void setPlatform(PropertySource<JavaPlatform> platform) {
            if (platform == null) throw new NullPointerException("platform");
            this.platform = platform;
        }

        public PropertySource<JavaPlatform> getScriptPlatform() {
            return scriptPlatform != null
                    ? scriptPlatform
                    : wrapSource(getPlatform(), true);
        }

        public void setScriptPlatform(PropertySource<JavaPlatform> scriptPlatform) {
            if (scriptPlatform == null) throw new NullPointerException("scriptPlatform");
            this.scriptPlatform = scriptPlatform;
        }

        private static PropertySource<GradleLocation> getGlobalGradleHomeAsFile(final boolean defaultValue) {
            return new PropertySource<GradleLocation>() {
                @Override
                public GradleLocation getValue() {
                    return GlobalGradleSettings.getGradleHome().getValue();
                }

                @Override
                public boolean isDefault() {
                    return defaultValue;
                }

                @Override
                public void addChangeListener(ChangeListener listener) {
                    GlobalGradleSettings.getGradleHome().addChangeListener(listener);
                }

                @Override
                public void removeChangeListener(ChangeListener listener) {
                    GlobalGradleSettings.getGradleHome().removeChangeListener(listener);
                }
            };
        }

        public PropertySource<GradleLocation> getGradleHome() {
            return gradleHome != null
                    ? gradleHome
                    : getGlobalGradleHomeAsFile(true);
        }

        public void setGradleHome(PropertySource<GradleLocation> gradleHome) {
            this.gradleHome = gradleHome;
        }

        public PropertySource<Charset> getSourceEncoding() {
            return sourceEncoding != null
                    ? sourceEncoding
                    : asConst(AbstractProjectProperties.DEFAULT_SOURCE_ENCODING, true);
        }

        public void setSourceEncoding(PropertySource<Charset> sourceEncoding) {
            if (sourceEncoding == null) throw new NullPointerException("sourceEncoding");
            this.sourceEncoding = sourceEncoding;
        }

        public PropertySource<List<PredefinedTask>> getCommonTasks() {
            return commonTasks != null
                    ? commonTasks
                    : asConst(Collections.<PredefinedTask>emptyList(), true);
        }

        public void setCommonTasks(PropertySource<List<PredefinedTask>> commonTasks) {
            if (commonTasks == null) throw new NullPointerException("commonTasks");
            this.commonTasks = commonTasks;
        }

        public PropertiesSnapshot create() {
            return new PropertiesSnapshot(this);
        }
    }

    private final PropertySource<String> sourceLevel;
    private final PropertySource<JavaPlatform> platform;
    private final PropertySource<JavaPlatform> scriptPlatform;
    private final PropertySource<GradleLocation> gradleHome;
    private final PropertySource<Charset> sourceEncoding;
    private final PropertySource<LicenseHeaderInfo> licenseHeader;
    private final PropertySource<List<PredefinedTask>> commonTasks;
    private final Map<String, PropertySource<PredefinedTask>> builtInTasks;
    private final List<AuxConfigSource> auxProperties;
    private final Map<DomElementKey, AuxConfigSource> auxPropertiesMap;

    public PropertiesSnapshot(ProjectProperties properties) {
        this.sourceLevel = asConst(properties.getSourceLevel());
        this.platform = asConst(properties.getPlatform());
        this.scriptPlatform = asConst(properties.getScriptPlatform());
        this.gradleHome = asConst(properties.getGradleLocation());
        this.sourceEncoding = asConst(properties.getSourceEncoding());
        this.commonTasks = asConst(properties.getCommonTasks());
        this.licenseHeader = asConst(properties.getLicenseHeader());

        Collection<AuxConfigProperty> otherAuxConfigs = properties.getAllAuxConfigs();
        this.auxProperties = new ArrayList<AuxConfigSource>(otherAuxConfigs.size());
        for (AuxConfigProperty auxProperty: otherAuxConfigs) {
            MutableProperty<Element> property = auxProperty.getProperty();
            this.auxProperties.add(new AuxConfigSource(
                    auxProperty.getKey(),
                    new DomElementSource(property.getValue(), property.isDefault())));
        }
        this.auxPropertiesMap = sourcesToMap(this.auxProperties);

        Set<String> commands = AbstractProjectProperties.getCustomizableCommands();
        this.builtInTasks = new HashMap<String, PropertySource<PredefinedTask>>(2 * commands.size());
        for (String command: commands) {
            MutableProperty<PredefinedTask> taskProperty = properties.tryGetBuiltInTask(command);
            if (taskProperty == null) {
                LOGGER.log(Level.WARNING, "ProjectProperties does not contain customizable command: {0}", command);
            }
            else {
                this.builtInTasks.put(command, asConst(taskProperty));
            }
        }
    }

    private static <ValueType> PropertySource<ValueType> asConst(MutableProperty<ValueType> property) {
        return asConst(property.getValue(), property.isDefault());
    }

    private static <ValueType> PropertySource<ValueType> wrapSource(
            final PropertySource<ValueType> source,
            final boolean defaultValue) {
        assert source != null;

        return new PropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return source.getValue();
            }

            @Override
            public boolean isDefault() {
                return defaultValue;
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                source.addChangeListener(listener);
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                source.removeChangeListener(listener);
            }
        };
    }

    private static <ValueType> PropertySource<ValueType> asConstNullForNull(ValueType value, boolean defaultValue) {
        return value != null ? asConst(value, defaultValue) : null;
    }

    private static <ValueType> PropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<ValueType>(value, defaultValue);
    }

    private static Map<DomElementKey, AuxConfigSource> sourcesToMap(Collection<AuxConfigSource> sources) {
        Map<DomElementKey, AuxConfigSource> map = new HashMap<DomElementKey, AuxConfigSource>(
                2 * sources.size() + 1);
        for (AuxConfigSource auxSource: sources) {
            map.put(auxSource.getKey(), auxSource);
        }
        return map;
    }

    private PropertiesSnapshot(Builder builder) {
        this.sourceLevel = builder.getSourceLevel();
        this.platform = builder.getPlatform();
        this.scriptPlatform = builder.getScriptPlatform();
        this.gradleHome = builder.getGradleHome();
        this.sourceEncoding = builder.getSourceEncoding();
        this.commonTasks = builder.getCommonTasks();
        this.licenseHeader = builder.getLicenseHeader();
        this.builtInTasks = new HashMap<String, PropertySource<PredefinedTask>>(builder.builtInTasks);
        this.auxProperties = Collections.unmodifiableList(new ArrayList<AuxConfigSource>(builder.auxProperties));
        this.auxPropertiesMap = sourcesToMap(this.auxProperties);
    }

    public PropertySource<LicenseHeaderInfo> getLicenseHeader() {
        return licenseHeader;
    }

    public PropertySource<String> getSourceLevel() {
        return sourceLevel;
    }

    public PropertySource<JavaPlatform> getPlatform() {
        return platform;
    }

    public PropertySource<JavaPlatform> getScriptPlatform() {
        return scriptPlatform;
    }

    public PropertySource<GradleLocation> getGradleHome() {
        return gradleHome;
    }

    public PropertySource<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    public PropertySource<List<PredefinedTask>> getCommonTasks() {
        return commonTasks;
    }

    public Collection<AuxConfigSource> getAuxProperties() {
        return auxProperties;
    }

    public AuxConfigSource getAuxProperty(DomElementKey key) {
        if (key == null) throw new NullPointerException("key");
        AuxConfigSource result = auxPropertiesMap.get(key);
        if (result == null) {
            result = new AuxConfigSource(key, new DomElementSource(null, true));
        }
        return result;
    }

    public PropertySource<PredefinedTask> tryGetBuiltInTask(String command) {
        if (command == null) throw new NullPointerException("command");
        PropertySource<PredefinedTask> result = builtInTasks.get(command);
        return result != null
                ? result
                : asConstNullForNull(BuiltInTasks.getDefaultBuiltInTask(command), true);
    }
}
