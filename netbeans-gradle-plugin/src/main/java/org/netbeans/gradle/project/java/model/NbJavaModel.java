package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaSourceSet;

public final class NbJavaModel {
    private final NbJavaModule mainModule;
    private final Map<File, JavaProjectDependency> projectDependencies;
    private final AtomicReference<Set<JavaProjectReference>> allDependenciesRef;

    private NbJavaModel(NbJavaModule mainModule, Map<File, JavaProjectDependency> projectDependencies) {
        this.mainModule = mainModule;
        this.projectDependencies = projectDependencies;
        this.allDependenciesRef = new AtomicReference<Set<JavaProjectReference>>(null);
    }

    private static void addAll(
            Collection<File> dependencies,
            Map<? extends File, ? extends JavaProjectDependency> possibleDependencies,
            Map<File, JavaProjectDependency> result) {

        for (File file: dependencies) {
            JavaProjectDependency dependency = possibleDependencies.get(file);
            if (dependency != null) {
                result.put(file, dependency);
            }
        }
    }

    public static NbJavaModel createModel(
            NbJavaModule mainModule,
            Map<? extends File, ? extends JavaProjectDependency> possibleDependencies) {
        if (mainModule == null) throw new NullPointerException("mainModule");
        if (possibleDependencies == null) throw new NullPointerException("possibleDependencies");

        Map<File, JavaProjectDependency> relevantDependencies
                = new HashMap<File, JavaProjectDependency>();
        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            JavaClassPaths classpaths = sourceSet.getClasspaths();

            addAll(classpaths.getCompileClasspaths(), possibleDependencies, relevantDependencies);
            addAll(classpaths.getRuntimeClasspaths(), possibleDependencies, relevantDependencies);
        }

        return new NbJavaModel(mainModule, relevantDependencies);
    }

    public JavaProjectDependency tryGetDepedency(File outputDir) {
        return projectDependencies.get(outputDir);
    }

    public NbJavaModule getMainModule() {
        return mainModule;
    }

    private Set<JavaProjectReference> extractAllDependencies() {
        Set<JavaProjectReference> result = new HashSet<JavaProjectReference>();
        for (JavaProjectDependency dependency: projectDependencies.values()) {
            result.add(dependency.getProjectReference());
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<JavaProjectReference> getAllDependencies() {
        Set<JavaProjectReference> result = allDependenciesRef.get();
        if (result == null) {
            allDependenciesRef.set(extractAllDependencies());
            result = allDependenciesRef.get();
        }
        return result;
    }
}
