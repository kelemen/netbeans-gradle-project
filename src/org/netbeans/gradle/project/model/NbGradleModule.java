package org.netbeans.gradle.project.model;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class NbGradleModule {
    private final Properties properties;
    private final Map<NbSourceType, NbSourceGroup> sources;
    private final Map<NbDependencyType, NbDependencyGroup> dependencies;
    private final List<NbGradleModule> children;
    private final List<File> listedDirs;

    // Should only be called by NbGradleModuleBuilder
    NbGradleModule(
            Properties properties,
            Map<NbSourceType, NbSourceGroup> sources,
            List<File> listedDirs,
            Map<NbDependencyType, NbDependencyGroup> dependencies,
            List<NbGradleModule> children) {

        if (properties == null) throw new NullPointerException("properties");
        if (dependencies == null) throw new NullPointerException("dependencies");
        if (listedDirs == null) throw new NullPointerException("listedDirs");
        if (children == null) throw new NullPointerException("children");

        this.properties = properties;
        this.sources = Collections.unmodifiableMap(sources);
        this.listedDirs = Collections.unmodifiableList(listedDirs);
        this.dependencies = Collections.unmodifiableMap(dependencies);
        this.children = Collections.unmodifiableList(children);
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

    public List<File> getListedDirs() {
        return listedDirs;
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
        private final List<String> nameParts;

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
            this.nameParts = Collections.unmodifiableList(new ArrayList<String>(
                    NbModelUtils.getNameParts(uniqueName)));
            this.name = this.nameParts.get(this.nameParts.size() - 1);

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

        public List<String> getNameParts() {
            return nameParts;
        }

        public Collection<NbGradleTask> getTasks() {
            return tasks;
        }
    }
}
