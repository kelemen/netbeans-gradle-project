package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.UpdateTaskExecutor;
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
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class JavaProjectDependencies {
    private final JavaExtension javaExt;
    private final UpdateTaskExecutor updateExecutor;

    private final MutableProperty<TranslatedDependencies> translatedDependencies;
    private final PropertySource<Map<File, ProjectDependencyCandidate>> translatedDependenciesMap;

    public JavaProjectDependencies(JavaExtension javaExt) {
        this(javaExt, NbTaskExecutors.DEFAULT_EXECUTOR);
    }

    public JavaProjectDependencies(JavaExtension javaExt, TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");
        this.javaExt = javaExt;
        this.updateExecutor = new GenericUpdateTaskExecutor(TaskExecutors.inOrderSimpleExecutor(executor));
        this.translatedDependencies = PropertyFactory
                .memPropertyConcurrent(null, true, SwingTaskExecutor.getStrictExecutor(true));
        this.translatedDependenciesMap = PropertyFactory.convert(translatedDependencies, new ValueConverter<TranslatedDependencies, Map<File, ProjectDependencyCandidate>>() {
            @Override
            public Map<File, ProjectDependencyCandidate> convert(TranslatedDependencies input) {
                return input != null
                        ? input.translatedDependencies
                        : Collections.<File, ProjectDependencyCandidate>emptyMap();
            }
        });
    }

    public void updateDependencies() {
        updateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateDependenciesNow();
            }
        });
    }

    public PropertySource<Map<File, ProjectDependencyCandidate>> translatedDependencies() {
        return translatedDependenciesMap;
    }

    public JavaProjectDependencyDef tryGetDependency(File output) {
        ProjectDependencyCandidate candidate = translatedDependenciesMap.getValue().get(output);
        return candidate != null ? candidate.tryGetDependency() : null;
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
        FileObject dependencyObj = FileUtil.toFileObject(dependency);
        if (dependencyObj == null) {
            return null;
        }

        Project owner = FileOwnerQuery.getOwner(dependencyObj);
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
