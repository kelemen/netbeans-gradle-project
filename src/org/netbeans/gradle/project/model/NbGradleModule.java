package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class NbGradleModule {
    private final Properties properties;
    private final Map<NbSourceType, NbSourceGroup> sources;
    private final Map<NbDependencyType, NbDependencyGroup> dependencies;

    public NbGradleModule(
            Properties properties,
            Map<NbSourceType, NbSourceGroup> sources,
            Map<NbDependencyType, NbDependencyGroup> dependencies) {
        if (properties == null) throw new NullPointerException("properties");
        if (dependencies == null) throw new NullPointerException("dependencies");

        this.properties = properties;
        this.sources = asImmutable(NbSourceType.class, sources);
        this.dependencies = asImmutable(NbDependencyType.class, dependencies);
    }

    private static <K extends Enum<K>, V> Map<K, V> asImmutable(
            Class<K> keyType,
            Map<K, V> map) {

        Map<K, V> clonedMap = new EnumMap<K, V>(keyType);
        clonedMap.putAll(map);

        for (Map.Entry<K, V> entry: clonedMap.entrySet()) {
            if (entry.getKey() == null) throw new NullPointerException("entry.key");
            if (entry.getValue() == null) throw new NullPointerException("entry.value for " + entry.getKey());
        }

        return Collections.unmodifiableMap(clonedMap);
    }

    private static String getNameFromUniqueName(String uniqueName) {
        int lastSepIndex = uniqueName.lastIndexOf(':');
        return lastSepIndex >= 0
                ? uniqueName.substring(lastSepIndex + 1)
                : uniqueName;
    }

    public Properties getProperties() {
        return properties;
    }

    public File getModuleDir() {
        return properties.getModuleDir();
    }

    public String getName() {
        return properties.getName();
    }

    public String getUniqueName() {
        return properties.getUniqueName();
    }

    public Collection<String> getTasks() {
        return properties.getTasks();
    }

    public NbSourceGroup getSources(NbSourceType sourceType) {
        NbSourceGroup result = sources.get(sourceType);
        return result != null ? result : NbSourceGroup.EMPTY;
    }

    public Map<NbSourceType, NbSourceGroup> getSources() {
        return sources;
    }

    public NbDependencyGroup getDependencies(NbDependencyType dependencyType) {
        NbDependencyGroup result = dependencies.get(dependencyType);
        return result != null ? result : NbDependencyGroup.EMPTY;
    }

    public Map<NbDependencyType, NbDependencyGroup> getDependencies() {
        return dependencies;
    }

    public static final class Properties {
        private final File moduleDir;
        private final String uniqueName;
        private final String name;
        private final Collection<String> tasks;

        public Properties(
                File moduleDir,
                String uniqueName,
                Collection<String> tasks) {
            if (moduleDir == null) throw new NullPointerException("moduleDir");
            if (uniqueName == null) throw new NullPointerException("uniqueName");
            if (tasks == null) throw new NullPointerException("tasks");

            this.moduleDir = moduleDir;
            this.uniqueName = uniqueName;
            this.name = getNameFromUniqueName(uniqueName);

            List<String> clonedTasks = new ArrayList<String>(new HashSet<String>(tasks));
            Collections.sort(clonedTasks);
            this.tasks = Collections.unmodifiableList(clonedTasks);

            for (String task: this.tasks) {
                if (task == null) throw new NullPointerException("task");
            }
        }

        public File getModuleDir() {
            return moduleDir;
        }

        public String getName() {
            return name;
        }

        public String getUniqueName() {
            return uniqueName;
        }

        public Collection<String> getTasks() {
            return tasks;
        }
    }
}
