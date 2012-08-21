package org.netbeans.gradle.project.model;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NbGradleModule {
    private final Properties properties;
    private final Map<NbSourceType, NbSourceGroup> sources;
    private final Map<NbDependencyType, NbDependencyGroup> dependencies;
    private final List<NbGradleModule> children;

    public NbGradleModule(
            Properties properties,
            Map<NbSourceType, NbSourceGroup> sources,
            Map<NbDependencyType, NbDependencyGroup> dependencies,
            Collection<NbGradleModule> children) {

        if (properties == null) throw new NullPointerException("properties");
        if (dependencies == null) throw new NullPointerException("dependencies");
        if (children == null) throw new NullPointerException("children");

        this.properties = properties;
        this.sources = asImmutable(NbSourceType.class, sources);
        this.dependencies = asImmutable(NbDependencyType.class, dependencies);
        this.children = Collections.unmodifiableList(new ArrayList<NbGradleModule>(children));

        for (NbGradleModule child: this.children) {
            if (child == null) throw new NullPointerException("child");
        }
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

    public Collection<NbGradleTask> getTasks() {
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

    public List<NbGradleModule> getChildren() {
        return children;
    }

    public static final class Properties {
        private static final Collator STR_CMP = Collator.getInstance();

        private final File moduleDir;
        private final NbOutput output;
        private final String uniqueName;
        private final String name;
        private final Collection<NbGradleTask> tasks;

        public Properties(
                String uniqueName,
                File moduleDir,
                NbOutput output,
                Collection<NbGradleTask> tasks) {
            if (uniqueName == null) throw new NullPointerException("uniqueName");
            if (moduleDir == null) throw new NullPointerException("moduleDir");
            if (output == null) throw new NullPointerException("output");
            if (tasks == null) throw new NullPointerException("tasks");

            this.uniqueName = uniqueName;
            this.moduleDir = moduleDir;
            this.output = output;
            this.name = getNameFromUniqueName(uniqueName);

            List<NbGradleTask> clonedTasks = new ArrayList<NbGradleTask>(tasks);
            Collections.sort(clonedTasks, new Comparator<NbGradleTask>() {
                @Override
                public int compare(NbGradleTask o1, NbGradleTask o2) {
                    return STR_CMP.compare(o1.getLocalName(), o2.getLocalName());
                }
            });
            this.tasks = Collections.unmodifiableList(clonedTasks);

            for (NbGradleTask task: this.tasks) {
                if (task == null) throw new NullPointerException("task");
            }
        }

        public NbOutput getOutput() {
            return output;
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

        public Collection<NbGradleTask> getTasks() {
            return tasks;
        }
    }
}
