package org.netbeans.gradle.project.java.model;

import com.google.common.collect.Maps;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.util.Utilities;

public final class JavaProjectDependencies {
    private static final PropertySource<Map<File, JavaProjectDependencyDef>> NO_DEPENDENCIES
            = PropertyFactory.constSource(Collections.<File, JavaProjectDependencyDef>emptyMap());

    private final JavaExtension javaExt;
    private final UpdateTaskExecutor updateExecutor;

    private final MutableProperty<TranslatedDependencies> translatedDependencies;
    private final PropertySource<Map<File, JavaProjectDependencyDef>> translatedJavaDependenciesMap;
    private final PropertySource<Map<File, JavaProjectDependencyDef>> filteredTranslatedJavaDependenciesMap;

    public JavaProjectDependencies(JavaExtension javaExt) {
        this(javaExt, NbTaskExecutors.DEFAULT_EXECUTOR);
    }

    public JavaProjectDependencies(JavaExtension javaExt, TaskExecutor executor) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.updateExecutor = new GenericUpdateTaskExecutor(TaskExecutors.inOrderSimpleExecutor(executor));
        this.translatedDependencies = PropertyFactory
                .memPropertyConcurrent(null, true, SwingExecutors.getStrictExecutor(true));
        this.translatedJavaDependenciesMap = PropertyFactory.propertyOfProperty(translatedDependencies, (TranslatedDependencies src) -> {
            return src != null
                    ? new ProjectDepedencyDefProperty(src.translatedDependencies)
                    : NO_DEPENDENCIES;
        });
        this.filteredTranslatedJavaDependenciesMap = PropertyFactory.convert(translatedJavaDependenciesMap, (input) -> {
            return Maps.filterValues(input, dependencyDef -> dependencyDef != null);
        });
    }

    private static final class ProjectDepedencyDefProperty
    implements
            PropertySource<Map<File, JavaProjectDependencyDef>> {
        private final Map<File, ProjectDependencyCandidate> src;

        public ProjectDepedencyDefProperty(Map<File, ProjectDependencyCandidate> src) {
            this.src = src != null
                    ? src
                    : Collections.<File, ProjectDependencyCandidate>emptyMap();
        }

        @Override
        public Map<File, JavaProjectDependencyDef> getValue() {
            return Maps.transformValues(src, input -> input.projectDependency().getValue());
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            List<ListenerRef> result = new ArrayList<>(src.size());
            for (ProjectDependencyCandidate candidate: src.values()) {
                candidate.projectDependency().addChangeListener(listener);
            }
            return ListenerRefs.combineListenerRefs(result);
        }
    }

    public void updateDependencies() {
        updateExecutor.execute(this::updateDependenciesNow);
    }

    public PropertySource<Map<File, JavaProjectDependencyDef>> translatedDependencies() {
        return filteredTranslatedJavaDependenciesMap;
    }

    public JavaProjectDependencyDef tryGetDependency(File output) {
        return translatedJavaDependenciesMap.getValue().get(output);
    }

    public void forAllCandidates(Consumer<? super ProjectDependencyCandidate> task) {
        TranslatedDependencies value = translatedDependencies.getValue();
        if (value != null) {
            for (ProjectDependencyCandidate candidate: value.translatedDependencies.values()) {
                task.accept(candidate);
            }
        }
    }

    private void updateDependenciesNow() {
        // This method is never called concurrently due to the update executor.

        NbJavaModule currentModule = javaExt.getCurrentModel().getMainModule();

        TranslatedDependencies currentTranslatedDependencies = translatedDependencies.getValue();
        if (currentTranslatedDependencies != null && currentTranslatedDependencies.source == currentModule) {
            return;
        }

        this.translatedDependencies.setValue(new TranslatedDependencies(currentModule, translateDependencies(currentModule)));
    }

    private static Map<File, ProjectDependencyCandidate> translateDependencies(NbJavaModule module) {
        Map<File, ProjectDependencyCandidate> result = new HashMap<>();

        for (JavaSourceSet sourceSet: module.getSources()) {
            JavaClassPaths classpaths = sourceSet.getClasspaths();

            Set<File> compileClasspaths = classpaths.getCompileClasspaths();
            for (File dependency: compileClasspaths) {
                tryTranslateDependency(dependency, result);
            }

            for (File dependency: classpaths.getRuntimeClasspaths()) {
                if (!compileClasspaths.contains(dependency)) {
                    tryTranslateDependency(dependency, result);
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    private static void tryTranslateDependency(File dependency, Map<File, ProjectDependencyCandidate> result) {
        ProjectDependencyCandidate translated = tryTranslateDependency(dependency);
        if (translated != null) {
            result.put(dependency, translated);
        }
    }

    private static ProjectDependencyCandidate tryTranslateDependency(File dependency) {
        URI dependencyUri = Utilities.toURI(dependency);
        Project owner = FileOwnerQuery.getOwner(dependencyUri);
        if (owner == null) {
            return null;
        }

        return new ProjectDependencyCandidate(owner, dependency);
    }

    private static final class TranslatedDependencies {
        public final NbJavaModule source;
        public final Map<File, ProjectDependencyCandidate> translatedDependencies;

        public TranslatedDependencies(
                NbJavaModule source,
                Map<File, ProjectDependencyCandidate> translatedDependencies) {
            this.source = source;
            this.translatedDependencies = translatedDependencies;
        }
    }
}
