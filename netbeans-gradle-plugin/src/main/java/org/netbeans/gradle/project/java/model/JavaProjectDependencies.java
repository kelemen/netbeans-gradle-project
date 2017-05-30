package org.netbeans.gradle.project.java.model;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbFunction;
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
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");
        this.javaExt = javaExt;
        this.updateExecutor = new GenericUpdateTaskExecutor(TaskExecutors.inOrderSimpleExecutor(executor));
        this.translatedDependencies = PropertyFactory
                .memPropertyConcurrent(null, true, SwingTaskExecutor.getStrictExecutor(true));
        this.translatedJavaDependenciesMap = NbProperties.propertyOfProperty(translatedDependencies, new NbFunction<TranslatedDependencies, PropertySource<Map<File, JavaProjectDependencyDef>>>() {
            @Override
            public PropertySource<Map<File, JavaProjectDependencyDef>> apply(TranslatedDependencies src) {
                return src != null
                        ? new ProjectDepedencyDefProperty(src.translatedDependencies)
                        : NO_DEPENDENCIES;
            }
        });
        this.filteredTranslatedJavaDependenciesMap = PropertyFactory.convert(translatedJavaDependenciesMap, new ValueConverter<Map<File, JavaProjectDependencyDef>, Map<File, JavaProjectDependencyDef>>() {
            @Override
            public Map<File, JavaProjectDependencyDef> convert(Map<File, JavaProjectDependencyDef> input) {
                return Maps.filterValues(input, new Predicate<JavaProjectDependencyDef>() {
                    @Override
                    public boolean apply(JavaProjectDependencyDef input) {
                        return input != null;
                    }
                });
            }
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
            return Maps.transformValues(src, new Function<ProjectDependencyCandidate, JavaProjectDependencyDef>() {
                @Override
                public JavaProjectDependencyDef apply(ProjectDependencyCandidate input) {
                    return input.projectDependency().getValue();
                }
            });
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            List<ListenerRef> result = new ArrayList<>(src.size());
            for (ProjectDependencyCandidate candidate: src.values()) {
                candidate.projectDependency().addChangeListener(listener);
            }
            return ListenerRegistries.combineListenerRefs(result);
        }
    }

    public void updateDependencies() {
        updateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateDependenciesNow();
            }
        });
    }

    public PropertySource<Map<File, JavaProjectDependencyDef>> translatedDependencies() {
        return filteredTranslatedJavaDependenciesMap;
    }

    public JavaProjectDependencyDef tryGetDependency(File output) {
        return translatedJavaDependenciesMap.getValue().get(output);
    }

    public void forAllCandidates(NbConsumer<? super ProjectDependencyCandidate> task) {
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
