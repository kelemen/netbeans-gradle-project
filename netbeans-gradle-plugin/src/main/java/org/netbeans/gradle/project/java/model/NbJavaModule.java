package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;

public final class NbJavaModule {
    private final GenericProjectProperties properties;
    private final JavaCompatibilityModel compatibilityModel;
    private final NbOutput outputDirs;
    private final Map<NbSourceType, NbSourceGroup> sources;
    private final Map<NbDependencyType, NbDependencyGroup> dependencies;
    private final List<File> listedDirs;

    // Should only be called by NbJavaModuleBuilder
    NbJavaModule(
            GenericProjectProperties properties,
            JavaCompatibilityModel compatibilityModel,
            NbOutput outputDirs,
            Map<NbSourceType, NbSourceGroup> sources,
            List<File> listedDirs,
            Map<NbDependencyType, NbDependencyGroup> dependencies) {

        if (properties == null) throw new NullPointerException("properties");
        if (compatibilityModel == null) throw new NullPointerException("compatibilityModel");
        if (outputDirs == null) throw new NullPointerException("outputDirs");
        if (dependencies == null) throw new NullPointerException("dependencies");
        if (listedDirs == null) throw new NullPointerException("listedDirs");

        this.properties = properties;
        this.compatibilityModel = compatibilityModel;
        this.outputDirs = outputDirs;
        this.sources = Collections.unmodifiableMap(sources);
        this.listedDirs = Collections.unmodifiableList(listedDirs);
        this.dependencies = Collections.unmodifiableMap(dependencies);
    }

    public GenericProjectProperties getProperties() {
        return properties;
    }

    public JavaCompatibilityModel getCompatibilityModel() {
        return compatibilityModel;
    }

    public NbOutput getOutputDirs() {
        return outputDirs;
    }


    public File getModuleDir() {
        return properties.getProjectDir();
    }

    public String getShortName() {
        return properties.getProjectName();
    }

    public String getUniqueName() {
        return properties.getProjectFullName();
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
}
