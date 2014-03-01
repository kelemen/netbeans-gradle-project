package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaSourceSet;

public final class NbJavaModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final JavaModelSource modelSource;
    private final NbJavaModule mainModule;
    private final Map<File, JavaProjectDependency> projectDependencies;
    private final AtomicReference<Set<JavaProjectReference>> allDependenciesRef;

    private NbJavaModel(
            JavaModelSource modelSource,
            NbJavaModule mainModule,
            Map<File, JavaProjectDependency> projectDependencies) {

        this.modelSource = modelSource;
        this.mainModule = mainModule;
        this.projectDependencies = projectDependencies;
        this.allDependenciesRef = new AtomicReference<>(null);
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
            JavaModelSource modelSource,
            NbJavaModule mainModule,
            Map<? extends File, ? extends JavaProjectDependency> possibleDependencies) {

        if (modelSource == null) throw new NullPointerException("modelSource");
        if (mainModule == null) throw new NullPointerException("mainModule");
        if (possibleDependencies == null) throw new NullPointerException("possibleDependencies");

        Map<File, JavaProjectDependency> relevantDependencies
                = new HashMap<>();
        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            JavaClassPaths classpaths = sourceSet.getClasspaths();

            addAll(classpaths.getCompileClasspaths(), possibleDependencies, relevantDependencies);
            addAll(classpaths.getRuntimeClasspaths(), possibleDependencies, relevantDependencies);
        }

        return new NbJavaModel(modelSource, mainModule, relevantDependencies);
    }

    public JavaProjectDependency tryGetDepedency(File outputDir) {
        return projectDependencies.get(outputDir);
    }

    public JavaModelSource getModelSource() {
        return modelSource;
    }

    public NbJavaModule getMainModule() {
        return mainModule;
    }

    private Set<JavaProjectReference> extractAllDependencies() {
        Set<JavaProjectReference> result = new HashSet<>();
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

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final JavaModelSource modelSource;
        private final NbJavaModule mainModule;
        private final Map<File, JavaProjectDependency> projectDependencies;

        public SerializedFormat(NbJavaModel source) {
            this.modelSource = source.modelSource;
            this.mainModule = source.mainModule;
            this.projectDependencies = source.projectDependencies;
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbJavaModel(modelSource, mainModule, projectDependencies);
        }
    }
}
