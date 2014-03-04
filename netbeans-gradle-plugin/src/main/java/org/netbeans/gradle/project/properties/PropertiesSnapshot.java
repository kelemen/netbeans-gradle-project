package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.w3c.dom.Element;

public final class PropertiesSnapshot {
    private static final Logger LOGGER = Logger.getLogger(PropertiesSnapshot.class.getName());

    public static final class Builder {
        private OldPropertySource<String> sourceLevel;
        private OldPropertySource<ProjectPlatform> platform;
        private OldPropertySource<JavaPlatform> scriptPlatform;
        private OldPropertySource<GradleLocation> gradleHome;
        private OldPropertySource<Charset> sourceEncoding;
        private OldPropertySource<LicenseHeaderInfo> licenseHeader;
        private OldPropertySource<List<PredefinedTask>> commonTasks;
        private final Map<String, OldPropertySource<PredefinedTask>> builtInTasks;
        private final List<AuxConfigSource> auxProperties;

        public Builder() {
            this.platform = null;
            this.sourceEncoding = null;
            this.sourceLevel = null;
            this.scriptPlatform = null;
            this.gradleHome = null;
            this.commonTasks = null;
            this.licenseHeader = null;
            this.builtInTasks = new HashMap<>();
            this.auxProperties = new LinkedList<>();
        }

        public void addAuxConfig(AuxConfig config, boolean defaultValue) {
            ExceptionHelper.checkNotNullArgument(config, "config");
            auxProperties.add(new AuxConfigSource(
                    config.getKey(),
                    new DomElementSource(config.getValue(), defaultValue)));
        }

        public void addAuxProperty(AuxConfigSource source) {
            ExceptionHelper.checkNotNullArgument(source, "source");
            auxProperties.add(source);
        }

        public void addAuxProperty(AuxConfigProperty auxProperty) {
            ExceptionHelper.checkNotNullArgument(auxProperty, "auxProperty");

            OldMutableProperty<Element> property = auxProperty.getProperty();
            auxProperties.add(new AuxConfigSource(
                    auxProperty.getKey(),
                    new DomElementSource(property.getValue(), property.isDefault())));
        }

        public Collection<AuxConfigSource> getAuxProperties() {
            return new ArrayList<>(auxProperties);
        }

        public void setBuiltInTask(String command, OldPropertySource<PredefinedTask> task) {
            ExceptionHelper.checkNotNullArgument(command, "command");
            ExceptionHelper.checkNotNullArgument(task, "task");

            builtInTasks.put(command, task);
        }

        public OldPropertySource<PredefinedTask> getBuiltInTask(String command) {
            ExceptionHelper.checkNotNullArgument(command, "command");

            OldPropertySource<PredefinedTask> result = builtInTasks.get(command);
            return result != null
                    ? result
                    : PropertiesSnapshot.<PredefinedTask>asConstNullForNull(null, true);
        }

        public OldPropertySource<LicenseHeaderInfo> getLicenseHeader() {
            return licenseHeader != null
                    ? licenseHeader
                    : asConst((LicenseHeaderInfo)null, true);
        }

        public void setLicenseHeader(OldPropertySource<LicenseHeaderInfo> licenseFile) {
            ExceptionHelper.checkNotNullArgument(licenseFile, "licenseFile");
            this.licenseHeader = licenseFile;
        }

        public OldPropertySource<String> getSourceLevel() {
            return sourceLevel != null
                    ? sourceLevel
                    : DefaultPropertySources.parseSourceLevelSource(getPlatform(), true);
        }

        public void setSourceLevel(OldPropertySource<String> sourceLevel) {
            ExceptionHelper.checkNotNullArgument(sourceLevel, "sourceLevel");
            this.sourceLevel = sourceLevel;
        }

        public OldPropertySource<ProjectPlatform> getPlatform() {
            return platform != null
                    ? platform
                    : asConst(AbstractProjectPlatformSource.getDefaultPlatform(), true);
        }

        public void setPlatform(OldPropertySource<ProjectPlatform> platform) {
            ExceptionHelper.checkNotNullArgument(platform, "platform");
            this.platform = platform;
        }

        public OldPropertySource<JavaPlatform> getScriptPlatform() {
            return scriptPlatform != null
                    ? scriptPlatform
                    : asConst(JavaPlatform.getDefault(), true);
        }

        public void setScriptPlatform(OldPropertySource<JavaPlatform> scriptPlatform) {
            ExceptionHelper.checkNotNullArgument(scriptPlatform, "scriptPlatform");
            this.scriptPlatform = scriptPlatform;
        }

        private static OldPropertySource<GradleLocation> getGlobalGradleHomeAsFile(final boolean defaultValue) {
            return new OldPropertySource<GradleLocation>() {
                @Override
                public GradleLocation getValue() {
                    return GlobalGradleSettings.getGradleHome().getValue();
                }

                @Override
                public boolean isDefault() {
                    return defaultValue;
                }

                @Override
                public ListenerRef addChangeListener(Runnable listener) {
                    return GlobalGradleSettings.getGradleHome().addChangeListener(listener);
                }
            };
        }

        public OldPropertySource<GradleLocation> getGradleHome() {
            return gradleHome != null
                    ? gradleHome
                    : getGlobalGradleHomeAsFile(true);
        }

        public void setGradleHome(OldPropertySource<GradleLocation> gradleHome) {
            this.gradleHome = gradleHome;
        }

        public OldPropertySource<Charset> getSourceEncoding() {
            return sourceEncoding != null
                    ? sourceEncoding
                    : asConst(AbstractProjectProperties.DEFAULT_SOURCE_ENCODING, true);
        }

        public void setSourceEncoding(OldPropertySource<Charset> sourceEncoding) {
            ExceptionHelper.checkNotNullArgument(sourceEncoding, "sourceEncoding");
            this.sourceEncoding = sourceEncoding;
        }

        public OldPropertySource<List<PredefinedTask>> getCommonTasks() {
            return commonTasks != null
                    ? commonTasks
                    : asConst(Collections.<PredefinedTask>emptyList(), true);
        }

        public void setCommonTasks(OldPropertySource<List<PredefinedTask>> commonTasks) {
            ExceptionHelper.checkNotNullArgument(commonTasks, "commonTasks");
            this.commonTasks = commonTasks;
        }

        public PropertiesSnapshot create() {
            return new PropertiesSnapshot(this);
        }
    }

    private final OldPropertySource<String> sourceLevel;
    private final OldPropertySource<ProjectPlatform> platform;
    private final OldPropertySource<JavaPlatform> scriptPlatform;
    private final OldPropertySource<GradleLocation> gradleHome;
    private final OldPropertySource<Charset> sourceEncoding;
    private final OldPropertySource<LicenseHeaderInfo> licenseHeader;
    private final OldPropertySource<List<PredefinedTask>> commonTasks;
    private final Map<String, OldPropertySource<PredefinedTask>> builtInTasks;
    private final List<AuxConfigSource> auxProperties;
    private final Map<DomElementKey, AuxConfigSource> auxPropertiesMap;
    private final Set<String> knownBuiltInCommands;

    public PropertiesSnapshot(ProjectProperties properties) {
        this.sourceLevel = asConst(properties.getSourceLevel());
        this.platform = asConst(properties.getPlatform());
        this.scriptPlatform = asConst(properties.getScriptPlatform());
        this.gradleHome = asConst(properties.getGradleLocation());
        this.sourceEncoding = asConst(properties.getSourceEncoding());
        this.commonTasks = asConst(properties.getCommonTasks());
        this.licenseHeader = asConst(properties.getLicenseHeader());

        Collection<AuxConfigProperty> otherAuxConfigs = properties.getAllAuxConfigs();
        this.auxProperties = new ArrayList<>(otherAuxConfigs.size());
        for (AuxConfigProperty auxProperty: otherAuxConfigs) {
            OldMutableProperty<Element> property = auxProperty.getProperty();
            this.auxProperties.add(new AuxConfigSource(
                    auxProperty.getKey(),
                    new DomElementSource(property.getValue(), property.isDefault())));
        }
        this.auxPropertiesMap = sourcesToMap(this.auxProperties);

        Set<String> commands = properties.getKnownBuiltInCommands();
        this.knownBuiltInCommands = Collections.unmodifiableSet(new HashSet<>(commands));
        this.builtInTasks = CollectionUtils.newHashMap(commands.size());
        for (String command: commands) {
            OldMutableProperty<PredefinedTask> taskProperty = properties.tryGetBuiltInTask(command);
            if (taskProperty == null) {
                LOGGER.log(Level.WARNING, "ProjectProperties does not contain customizable command: {0}", command);
            }
            else {
                this.builtInTasks.put(command, asConst(taskProperty));
            }
        }
    }

    private static <ValueType> OldPropertySource<ValueType> asConst(OldMutableProperty<ValueType> property) {
        return asConst(property.getValue(), property.isDefault());
    }

    private static <ValueType> OldPropertySource<ValueType> asConstNullForNull(ValueType value, boolean defaultValue) {
        return value != null ? asConst(value, defaultValue) : null;
    }

    private static <ValueType> OldPropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<>(value, defaultValue);
    }

    private static Map<DomElementKey, AuxConfigSource> sourcesToMap(Collection<AuxConfigSource> sources) {
        Map<DomElementKey, AuxConfigSource> map = CollectionUtils.newHashMap(sources.size());
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
        this.builtInTasks = new HashMap<>(builder.builtInTasks);
        this.knownBuiltInCommands = Collections.unmodifiableSet(builtInTasks.keySet());
        this.auxProperties = Collections.unmodifiableList(new ArrayList<>(builder.auxProperties));
        this.auxPropertiesMap = sourcesToMap(this.auxProperties);
    }

    public OldPropertySource<LicenseHeaderInfo> getLicenseHeader() {
        return licenseHeader;
    }

    public OldPropertySource<String> getSourceLevel() {
        return sourceLevel;
    }

    public OldPropertySource<ProjectPlatform> getPlatform() {
        return platform;
    }

    public OldPropertySource<JavaPlatform> getScriptPlatform() {
        return scriptPlatform;
    }

    public OldPropertySource<GradleLocation> getGradleHome() {
        return gradleHome;
    }

    public OldPropertySource<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    public OldPropertySource<List<PredefinedTask>> getCommonTasks() {
        return commonTasks;
    }

    public Collection<AuxConfigSource> getAuxProperties() {
        return auxProperties;
    }

    public AuxConfigSource getAuxProperty(DomElementKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        AuxConfigSource result = auxPropertiesMap.get(key);
        if (result == null) {
            result = new AuxConfigSource(key, new DomElementSource(null, true));
        }
        return result;
    }

    public OldPropertySource<PredefinedTask> tryGetBuiltInTask(String command) {
        ExceptionHelper.checkNotNullArgument(command, "command");

        OldPropertySource<PredefinedTask> result = builtInTasks.get(command);
        return result != null
                ? result
                : PropertiesSnapshot.<PredefinedTask>asConstNullForNull(null, true);
    }

    public Map<String, OldPropertySource<PredefinedTask>> getBuiltInTasks() {
        return Collections.unmodifiableMap(builtInTasks);
    }

    public Set<String> getKnownBuiltInCommands() {
        return knownBuiltInCommands;
    }
}
