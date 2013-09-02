package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.gradle.tooling.model.GradleProject;

public final class NbJavaModule {
    private final GradleProject gradleProject;
    private final Properties properties;
    private final Map<NbSourceType, NbSourceGroup> sources;
    private final Map<NbDependencyType, NbDependencyGroup> dependencies;
    private final List<File> listedDirs;

    // Should only be called by NbJavaModuleBuilder
    NbJavaModule(
            GradleProject gradleProject,
            Properties properties,
            Map<NbSourceType, NbSourceGroup> sources,
            List<File> listedDirs,
            Map<NbDependencyType, NbDependencyGroup> dependencies) {

        if (gradleProject == null) throw new NullPointerException("gradleProject");
        if (properties == null) throw new NullPointerException("properties");
        if (dependencies == null) throw new NullPointerException("dependencies");
        if (listedDirs == null) throw new NullPointerException("listedDirs");

        this.gradleProject = gradleProject;
        this.properties = properties;
        this.sources = Collections.unmodifiableMap(sources);
        this.listedDirs = Collections.unmodifiableList(listedDirs);
        this.dependencies = Collections.unmodifiableMap(dependencies);
    }

    public GradleProject getGradleProject() {
        return gradleProject;
    }

    public Properties getProperties() {
        return properties;
    }

    public File getModuleDir() {
        return properties.getModuleDir();
    }

    public String getShortName() {
        return properties.getShortName();
    }

    public String getUniqueName() {
        return properties.getUniqueName();
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

    public static final class Properties {
        private final String shortName;
        private final File moduleDir;
        private final NbOutput output;
        private final String uniqueName;
        private final String sourceLevel;
        private final String targetLevel;

        public Properties(
                String shortName,
                String uniqueName,
                File moduleDir,
                NbOutput output,
                String sourceLevel,
                String targetLevel) {

            if (shortName == null) throw new NullPointerException("scriptDisplayName");
            if (uniqueName == null) throw new NullPointerException("uniqueName");
            if (moduleDir == null) throw new NullPointerException("moduleDir");
            if (output == null) throw new NullPointerException("output");
            if (sourceLevel == null) throw new NullPointerException("sourceLevel");
            if (targetLevel == null) throw new NullPointerException("targetLevel");

            this.shortName = shortName;
            this.uniqueName = uniqueName;
            this.moduleDir = moduleDir;
            this.output = output;
            this.sourceLevel = sourceLevel;
            this.targetLevel = targetLevel;
        }

        public String getShortName() {
            return shortName;
        }

        public String getSourceLevel() {
            return sourceLevel;
        }

        public String getTargetLevel() {
            return targetLevel;
        }

        public NbOutput getOutput() {
            return output;
        }

        public File getModuleDir() {
            return moduleDir;
        }

        public String getUniqueName() {
            return uniqueName;
        }
    }
}
