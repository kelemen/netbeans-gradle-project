package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.gradle.tooling.model.GradleProject;
import org.netbeans.gradle.project.CollectionUtils;

public final class NbJavaModuleBuilder {
    // We need this class because in case of circular dependencies we are
    // not able to create an immutable NbJavaModule instance.

    private final Map<NbDependencyType, NbDependencyGroup> dependencies;
    private final NbJavaModule view;

    public NbJavaModuleBuilder(
            GradleProject gradleProject,
            NbJavaModule.Properties properties,
            Map<NbSourceType, NbSourceGroup> sources,
            List<File> listedDirs) {

        if (properties == null) throw new NullPointerException("properties");
        if (sources == null) throw new NullPointerException("sources");
        if (listedDirs == null) throw new NullPointerException("listedDirs");

        this.dependencies = new EnumMap<NbDependencyType, NbDependencyGroup>(NbDependencyType.class);
        this.view = new NbJavaModule(
                properties,
                copyNullSafeMutableMap(NbSourceType.class, sources),
                CollectionUtils.copyNullSafeMutableList(listedDirs),
                dependencies);
    }

    private static <K extends Enum<K>, V> Map<K, V> copyNullSafeMutableMap(
            Class<K> keyType,
            Map<K, V> map) {

        Map<K, V> clonedMap = new EnumMap<K, V>(keyType);
        clonedMap.putAll(map);

        for (Map.Entry<K, V> entry: clonedMap.entrySet()) {
            if (entry.getKey() == null) throw new NullPointerException("entry.key");
            if (entry.getValue() == null) throw new NullPointerException("entry.value for " + entry.getKey());
        }

        return clonedMap;
    }

    public void addDependencies(Map<NbDependencyType, NbDependencyGroup> dependencies) {
        for (Map.Entry<?, ?> entry: dependencies.entrySet()) {
            if (entry.getKey() == null) throw new NullPointerException("entry.key");
            if (entry.getValue() == null) throw new NullPointerException("entry.value for " + entry.getKey());
        }

        this.dependencies.putAll(dependencies);
    }

    public NbJavaModule getReadOnlyView() {
        return view;
    }
}
