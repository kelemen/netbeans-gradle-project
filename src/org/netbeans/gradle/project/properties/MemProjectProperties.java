package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.tasks.BuiltInTasks;

public final class MemProjectProperties extends AbstractProjectProperties {
    private final MutableProperty<String> sourceLevel;
    private final MutableProperty<JavaPlatform> platform;
    private final MutableProperty<Charset> sourceEncoding;
    private final MutableProperty<List<PredefinedTask>> commonTasks;
    private final Map<String, MutableProperty<PredefinedTask>> builtInTasks;

    public MemProjectProperties() {
        JavaPlatform defaultPlatform = JavaPlatform.getDefault();
        this.sourceLevel = new DefaultMutableProperty<String>(getSourceLevelFromPlatform(defaultPlatform), true, false);
        this.platform = new DefaultMutableProperty<JavaPlatform>(defaultPlatform, true, false);
        this.sourceEncoding = new DefaultMutableProperty<Charset>(DEFAULT_SOURCE_ENCODING, true, false);
        this.commonTasks = new MutableListProperty<PredefinedTask>(Collections.<PredefinedTask>emptyList(), true);

        Set<String> commands = AbstractProjectProperties.getCustomizableCommands();
        this.builtInTasks = new HashMap<String, MutableProperty<PredefinedTask>>(2 * commands.size());
        for (String command: commands) {
            PredefinedTask task = BuiltInTasks.getDefaultBuiltInTask(command);
            this.builtInTasks.put(command,
                    new DefaultMutableProperty<PredefinedTask>(task, true, false));
        }
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
}
