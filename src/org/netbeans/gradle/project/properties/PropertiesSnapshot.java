package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.tasks.BuiltInTasks;

public final class PropertiesSnapshot {
    private static final Logger LOGGER = Logger.getLogger(PropertiesSnapshot.class.getName());

    public static final class Builder {
        private PropertySource<String> sourceLevel;
        private PropertySource<JavaPlatform> platform;
        private PropertySource<Charset> sourceEncoding;
        private PropertySource<List<PredefinedTask>> commonTasks;
        private final Map<String, PropertySource<PredefinedTask>> builtInTasks;

        public Builder() {
            this.platform = null;
            this.sourceEncoding = null;
            this.sourceLevel = null;
            this.commonTasks = null;
            this.builtInTasks = new HashMap<String, PropertySource<PredefinedTask>>();
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
    private final PropertySource<Charset> sourceEncoding;
    private final PropertySource<List<PredefinedTask>> commonTasks;
    private final Map<String, PropertySource<PredefinedTask>> builtInTasks;

    public PropertiesSnapshot(ProjectProperties properties) {
        this.sourceLevel = asConst(properties.getSourceLevel());
        this.platform = asConst(properties.getPlatform());
        this.sourceEncoding = asConst(properties.getSourceEncoding());
        this.commonTasks = asConst(properties.getCommonTasks());

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

    private static <ValueType> PropertySource<ValueType> asConstNullForNull(ValueType value, boolean defaultValue) {
        return value != null ? asConst(value, defaultValue) : null;
    }

    private static <ValueType> PropertySource<ValueType> asConst(ValueType value, boolean defaultValue) {
        return new ConstPropertySource<ValueType>(value, defaultValue);
    }

    private PropertiesSnapshot(Builder builder) {
        this.sourceLevel = builder.getSourceLevel();
        this.platform = builder.getPlatform();
        this.sourceEncoding = builder.getSourceEncoding();
        this.commonTasks = builder.getCommonTasks();
        this.builtInTasks = new HashMap<String, PropertySource<PredefinedTask>>(builder.builtInTasks);
    }

    public PropertySource<String> getSourceLevel() {
        return sourceLevel;
    }

    public PropertySource<JavaPlatform> getPlatform() {
        return platform;
    }

    public PropertySource<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    public PropertySource<List<PredefinedTask>> getCommonTasks() {
        return commonTasks;
    }

    public PropertySource<PredefinedTask> tryGetBuiltInTask(String command) {
        if (command == null) throw new NullPointerException("command");
        PropertySource<PredefinedTask> result = builtInTasks.get(command);
        return result != null
                ? result
                : asConstNullForNull(BuiltInTasks.getDefaultBuiltInTask(command), true);
    }
}
